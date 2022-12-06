/*
 *     KookBC -- The Kook Bot Client & JKook API standard implementation for Java.
 *     Copyright (C) 2022 KookBC contributors
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU Affero General Public License as published
 *     by the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU Affero General Public License for more details.
 *
 *     You should have received a copy of the GNU Affero General Public License
 *     along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package snw.kookbc.impl;

import snw.jkook.Core;
import snw.jkook.command.CommandExecutor;
import snw.jkook.command.ConsoleCommandSender;
import snw.jkook.command.JKookCommand;
import snw.jkook.config.ConfigurationSection;
import snw.jkook.entity.User;
import snw.jkook.message.TextChannelMessage;
import snw.jkook.message.component.MarkdownComponent;
import snw.jkook.message.component.TextComponent;
import snw.jkook.plugin.Plugin;
import snw.jkook.plugin.PluginDescription;
import snw.jkook.util.Validate;
import snw.kookbc.impl.command.CommandManagerImpl;
import snw.kookbc.impl.console.Console;
import snw.kookbc.impl.entity.builder.EntityBuilder;
import snw.kookbc.impl.entity.builder.EntityUpdater;
import snw.kookbc.impl.entity.builder.MessageBuilder;
import snw.kookbc.impl.event.EventManagerImpl;
import snw.kookbc.impl.event.InternalEventListener;
import snw.kookbc.impl.network.Connector;
import snw.kookbc.impl.network.HttpAPIRoute;
import snw.kookbc.impl.network.NetworkClient;
import snw.kookbc.impl.network.Session;
import snw.kookbc.impl.scheduler.SchedulerImpl;
import snw.kookbc.impl.storage.EntityStorage;
import snw.kookbc.impl.tasks.BotMarketPingThread;
import snw.kookbc.impl.tasks.UpdateChecker;
import snw.kookbc.util.PrefixThreadFactory;

import java.io.File;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.stream.Collectors;

// The client representation.
public class KBCClient {
    private volatile boolean running = true;
    private final Core core;
    private final NetworkClient networkClient;
    private final EntityStorage storage;
    private final EntityBuilder entityBuilder;
    private final MessageBuilder msgBuilder;
    private final EntityUpdater entityUpdater;
    private final ConfigurationSection config;
    private final File pluginsFolder;
    private final Session session = new Session(null);
    protected final ExecutorService eventExecutor;
    protected Connector connector;

    public KBCClient(CoreImpl core, ConfigurationSection config, File pluginsFolder, String token) {
        if (pluginsFolder != null) {
            Validate.isTrue(pluginsFolder.isDirectory(), "The provided pluginsFolder object is not a directory.");
        }
        this.core = core;
        this.config = config;
        this.pluginsFolder = pluginsFolder;
        this.networkClient = new NetworkClient(this, token);
        this.storage = new EntityStorage(this);
        this.entityBuilder = new EntityBuilder(this);
        this.msgBuilder = new MessageBuilder(this);
        this.entityUpdater = new EntityUpdater(this);
        this.eventExecutor = Executors.newCachedThreadPool(new PrefixThreadFactory("Event Executor #"));
        core.init(this, new HttpAPIImpl(this, token));
    }

    // The result of this method can prevent the users to execute the console command,
    //  so that some possible problems won't be caused.
    // (e.g. Kook user stopped the client)
    private CommandExecutor wrapConsoleCmd(Consumer<Object[]> reallyThingToRun) {
        return (sender, arguments, message) -> {
            if (sender instanceof User) {
                if (getConfig().getBoolean("ignore-remote-call-invisible-internal-command", true)) {
                    return;
                }
                if (message instanceof TextChannelMessage) {
                    ((TextChannelMessage) message).getChannel().sendComponent(
                            new TextComponent("你不能这样做，因为你正在尝试执行仅后台可用的命令。"),
                            null,
                            message.getSender()
                    );
                } else {
                    ((User) sender).sendPrivateMessage(
                            new TextComponent("你不能这样做，因为你正在尝试执行仅后台可用的命令。")
                    );
                }
            } else {
                reallyThingToRun.accept(arguments);
            }
        };
    }

    public ConfigurationSection getConfig() {
        return config;
    }

    public Core getCore() {
        return core;
    }

    public File getPluginsFolder() {
        return pluginsFolder;
    }

    public boolean isRunning() {
        return running;
    }

    // Note for hardcore developers:
    // You can also put this client into your project as a module to communicate with Kook
    // Call this to start KookBC, then you can use JKook API.
    // WARN: Set the JKook Core by constructing CoreImpl and call getCore().setCore() using it first,
    // or you will get NullPointerException.
    public void start() {
        core.getLogger().debug("Fetching Bot user object");
        User botUser = getEntityBuilder().buildUser(
            getNetworkClient().get(HttpAPIRoute.USER_ME.toFullURL())
        );
        getStorage().addUser(botUser);
        core.setUser(botUser);
        getCore().getLogger().debug("Loading all the plugins from plugins folder.");
        loadAllPlugins();
        getCore().getLogger().debug("Starting Network");
        startNetwork();
        finishStart();
        getCore().getLogger().info("Done! Type \"help\" for help.");

        new UpdateChecker(this).start(); // check update. Added since 2022/7/24
    }

    protected void loadAllPlugins() {
        if (pluginsFolder == null) {
            return; // If you just want to use JKook API?
        }
        List<Plugin> plugins = new LinkedList<>(Arrays.asList(getCore().getPluginManager().loadPlugins(getPluginsFolder())));
        // we must call onLoad() first.
        for (Iterator<Plugin> iterator = plugins.iterator(); iterator.hasNext(); ) {
            Plugin plugin = iterator.next();

            // onLoad
            PluginDescription description = plugin.getDescription();
            plugin.getLogger().info("Loading {} version {}", description.getName(), description.getVersion());
            try {
                plugin.onLoad();
            } catch (Exception e) {
                getCore().getLogger().error("Unable to call Plugin#onLoad for a plugin.", e);
                iterator.remove();
            }
            // end onLoad
        }
        for (Iterator<Plugin> iterator = plugins.iterator(); iterator.hasNext(); ) {
            Plugin plugin = iterator.next();

            plugin.reloadConfig(); // ensure the default configuration will be loaded

            // onEnable
            getCore().getPluginManager().enablePlugin(plugin);
            if (!plugin.isEnabled()) {
                iterator.remove();
            }
            // end onEnable
        }
        plugins.forEach(getCore().getPluginManager()::addPlugin);
    }

    protected void startNetwork() {
        connector = new Connector(this);
        connector.start();
    }

    protected void finishStart() {
        registerInternal();
        // region BotMarket support part - 2022/7/28
        String rawBotMarketUUID = getConfig().getString("botmarket-uuid");
        if (rawBotMarketUUID != null) {
            if (!rawBotMarketUUID.isEmpty()) {
                try {
                    UUID.fromString(rawBotMarketUUID);
                    new BotMarketPingThread(this, rawBotMarketUUID).start();
                } catch (IllegalArgumentException e) {
                    getCore().getLogger().warn("Invalid UUID of BotMarket. We won't schedule the PING task for BotMarket.");
                }
            }
        }
        // endregion
    }

    // If you need console (normally you won't need it), call this
    // Note that this method won't return until the client stopped,
    // so call it in a single thread.
    public void loop() {
        getCore().getLogger().debug("Starting console");
        try {
            new Console(this).start();
        } catch (Exception e) {
            getCore().getLogger().error("Unexpected situation happened during the main loop.", e);
        }
        getCore().getLogger().debug("REPL end");
    }

    // Shutdown this client, and loop() method will return after this method completes.
    public void shutdown() {
        getCore().getLogger().debug("Client shutdown request received");
        if (!isRunning()) {
            getCore().getLogger().debug("The client has already stopped");
            return;
        }
        running = false; // make sure the client will shut down if Bot wish the client stop.

        getCore().getLogger().info("Stopping client");
        getCore().getPluginManager().clearPlugins();

        shutdownNetwork();
        eventExecutor.shutdown();
        getCore().getLogger().info("Stopping core");
        getCore().getLogger().info("Stopping scheduler (If the application got into infinite loop, please kill this process!)");
        ((SchedulerImpl) getCore().getScheduler()).shutdown();
        getCore().getLogger().info("Client stopped");
    }

    protected void shutdownNetwork() {
        if (connector != null) {
            connector.shutdown();
        }
    }

    public EntityStorage getStorage() {
        return storage;
    }

    public EntityBuilder getEntityBuilder() {
        return entityBuilder;
    }

    public MessageBuilder getMessageBuilder() {
        return msgBuilder;
    }

    public EntityUpdater getEntityUpdater() {
        return entityUpdater;
    }

    public Connector getConnector() {
        return connector;
    }

    public NetworkClient getNetworkClient() {
        return networkClient;
    }

    public Session getSession() {
        return session;
    }

    public ExecutorService getEventExecutor() {
        return eventExecutor;
    }

    protected void registerInternal() {
        registerCommand(
                new JKookCommand("stop")
                        .setDescription("停止 KookBC 实例。")
                        .setExecutor(wrapConsoleCmd((args) -> shutdown()))
        );
        registerHelpCommand();
        registerPluginsCommand();
        ((EventManagerImpl) getCore().getEventManager()).registerHandlers0(null, new InternalEventListener());
    }

    protected void registerPluginsCommand() {
        registerCommand(
                new JKookCommand("plugins")
                        .setDescription("获取已安装到此 KookBC 实例的插件列表。")
                        .setExecutor(
                                (sender, arguments, message) -> {
                                    String result = String.format(
                                            "已安装并正在运行的插件 (%s): %s",
                                            getCore().getPluginManager().getPlugins().length,
                                            String.join(", ",
                                                    Arrays.stream(getCore().getPluginManager().getPlugins())
                                                            .map(IT -> IT.getDescription().getName())
                                                            .collect(Collectors.toSet())
                                            )
                                    );
                                    if (sender instanceof User) {
                                        if (message instanceof TextChannelMessage) {
                                            ((TextChannelMessage) message).getChannel().sendComponent(
                                                    new MarkdownComponent(result),
                                                    null,
                                                    (User) sender
                                            );
                                        } else {
                                            ((User) sender).sendPrivateMessage(new MarkdownComponent(result));
                                        }
                                    } else {
                                        getCore().getLogger().info(result);
                                    }
                                }
                        )
        );
    }

    protected void registerHelpCommand() {
        registerCommand(
                new JKookCommand("help")
                        .setDescription("获取此帮助列表。")
                        .setExecutor(
                                (commandSender, args, message) -> {
                                    JKookCommand[] result;
                                    if (args.length != 0) {
                                        String helpWanted = (String) args[0];
                                        JKookCommand command = ((CommandManagerImpl) getCore().getCommandManager()).getCommand(helpWanted);
                                        if (command == null) {
                                            if (commandSender instanceof User) {
                                                if (message instanceof TextChannelMessage) {
                                                    ((TextChannelMessage) message).getChannel().sendComponent(
                                                            new MarkdownComponent("找不到命令。"),
                                                            null,
                                                            (User) commandSender
                                                    );
                                                } else {
                                                    ((User) commandSender).sendPrivateMessage(
                                                            new MarkdownComponent("找不到命令。")
                                                    );
                                                }
                                            } else if (commandSender instanceof ConsoleCommandSender) {
                                                getCore().getLogger().info("Unknown command.");
                                            }
                                            return;
                                        }
                                        result = new JKookCommand[]{command};
                                    } else {
                                        result = ((CommandManagerImpl) getCore().getCommandManager()).getCommandSet().toArray(new JKookCommand[0]);
                                    }

                                    List<String> helpList = getHelp(result);

                                    if (commandSender instanceof ConsoleCommandSender) {
                                        for (String s : helpList) {
                                            getCore().getLogger().info(s);
                                        }
                                    } else if (commandSender instanceof User) {
                                        helpList.removeIf(IT -> IT.startsWith("(/)stop:"));

                                        if (getConfig().getBoolean("allow-help-ad", true)) {
                                            helpList.add("由 [KookBC](https://github.com/SNWCreations/KookBC) v" + getCore().getImplementationVersion() + " 驱动 - JKook API " + getCore().getAPIVersion());
                                        } else {
                                            helpList.remove(helpList.size() - 1);
                                        }

                                        String finalResult = String.join("\n", helpList.toArray(new String[0]));
                                        if (message != null) {
                                            if (message instanceof TextChannelMessage) {
                                                ((TextChannelMessage) message).getChannel().sendComponent(
                                                        new MarkdownComponent(finalResult),
                                                        null,
                                                        (User) commandSender
                                                );
                                            } else {
                                                ((User) commandSender).sendPrivateMessage(new MarkdownComponent(finalResult));
                                            }
                                        } else {
                                            ((User) commandSender).sendPrivateMessage(new MarkdownComponent(finalResult));
                                        }
                                    }
                                }
                        )
        );
    }

    public static List<String> getHelp(JKookCommand[] commands) {
        if (commands.length <= 0) { // I think this is impossible to happen!
            return Collections.singletonList("无法提供命令帮助。因为此 KookBC 实例没有注册任何命令。");
        }
        List<String> result = new LinkedList<>();
        result.add("-------- 命令帮助 --------");
        if (commands.length > 1) {
            for (JKookCommand command : commands) {
                result.add(String.format("(%s)%s: %s", String.join(",", command.getPrefixes()), command.getRootName(),
                        (command.getDescription() == null) ? "此命令没有简介。" : command.getDescription()
                ));
            }
            result.add(""); // the blank line as the separator
            result.add("注: 在每条命令帮助的开头，括号中用 \",\" 隔开的字符为此命令的前缀。");
            result.add("如 \"(/,.)blah\" 即 \"/blah\", \".blah\" 为同一条命令。");
        } else {
            JKookCommand command = commands[0];
            result.add(String.format("命令: %s", command.getRootName()));
            result.add(String.format("别称: %s", String.join(", ", command.getAliases())));
            result.add(String.format("可用前缀: %s", String.join(", ", command.getPrefixes())));
            result.add(
                    String.format("简介: %s",
                            (command.getDescription() == null)
                                    ? "此命令没有简介。"
                                    : command.getDescription()
                    )
            );
            if (command.getHelpContent() != null && !command.getHelpContent().isEmpty()) {
                result.add("详细帮助信息:");
                result.add(command.getHelpContent());
            }
        }
        result.add("-------------------------");
        return result;
    }

    private void registerCommand(JKookCommand command) {
        ((CommandManagerImpl) getCore().getCommandManager()).getCommands().put(command, null);
    }
}
