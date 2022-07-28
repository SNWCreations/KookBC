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

import okhttp3.Request;
import okhttp3.RequestBody;
import snw.jkook.Core;
import snw.jkook.JKook;
import snw.jkook.bot.Bot;
import snw.jkook.bot.BotDescription;
import snw.jkook.command.CommandExecutor;
import snw.jkook.command.ConsoleCommandSender;
import snw.jkook.command.JKookCommand;
import snw.jkook.config.file.YamlConfiguration;
import snw.jkook.entity.User;
import snw.jkook.message.TextChannelMessage;
import snw.jkook.message.component.MarkdownComponent;
import snw.jkook.message.component.TextComponent;
import snw.jkook.scheduler.JKookRunnable;
import snw.jkook.util.Validate;
import snw.kookbc.impl.bot.SimpleBotClassLoader;
import snw.kookbc.impl.command.CommandManagerImpl;
import snw.kookbc.impl.console.Console;
import snw.kookbc.impl.entity.builder.EntityBuilder;
import snw.kookbc.impl.entity.builder.EntityUpdater;
import snw.kookbc.impl.entity.builder.MessageBuilder;
import snw.kookbc.impl.event.InternalEventListener;
import snw.kookbc.impl.network.Connector;
import snw.kookbc.impl.network.HttpAPIRoute;
import snw.kookbc.impl.network.NetworkClient;
import snw.kookbc.impl.storage.EntityStorage;
import snw.kookbc.impl.tasks.UpdateChecker;

import java.io.File;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import static snw.kookbc.util.Util.getVersionDifference;

// The client representation.
public class KBCClient {
    private static KBCClient INSTANCE = null;
    private final Core core;
    private final EntityStorage storage;
    private final EntityBuilder entityBuilder;
    private final MessageBuilder msgBuilder;
    private final EntityUpdater entityUpdater;
    private final YamlConfiguration config;
    private final File botDataFolder;
    private Bot bot;
    protected Connector connector;

    public KBCClient(Core core, YamlConfiguration config, File botDataFolder) {
        Validate.isTrue(botDataFolder.isDirectory(), "The provided botDataFolder object is not a directory.");
        this.core = core;
        this.config = config;
        this.botDataFolder = botDataFolder;
        storage = new EntityStorage(this);
        entityBuilder = new EntityBuilder(this);
        msgBuilder = new MessageBuilder(this);
        entityUpdater = new EntityUpdater();
        // setInstance(this); // make sure the instance can be used from other place
    }

    // Use this to access the most things in KookBC!
    public static KBCClient getInstance() {
        return INSTANCE;
    }

    public static void setInstance(KBCClient client) {
        Validate.notNull(client, "You can't define the singleton instance of KBCClient using null.");
        if (KBCClient.INSTANCE != null) {
            throw new IllegalStateException("Cannot re-define the singleton instance of KBCClient.");
        }
        KBCClient.INSTANCE = client;
    }

    // The result of this method can prevent the users to execute the console command,
    //  so that some possible problems won't be caused.
    // (e.g. Kook user stopped the client)
    private CommandExecutor wrapConsoleCmd(Consumer<String[]> reallyThingToRun) {
        return (sender, arguments, message) -> {
            if (sender instanceof User) {
                if (getConfig().getBoolean("ignore-remote-call-invisible-internal-command", true)) {
                    return;
                }
                if (message != null) {
                    if (message instanceof TextChannelMessage) {
                        ((TextChannelMessage) message).getChannel().sendComponent(
                                new TextComponent("你不能这样做，因为你正在尝试执行仅后台可用的命令。"),
                                null,
                                message.getSender()
                        );
                    } else {
                        message.getSender().sendPrivateMessage(
                                new TextComponent("你不能这样做，因为你正在尝试执行仅后台可用的命令。")
                        );
                    }
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

    public YamlConfiguration getConfig() {
        return config;
    }

    public Core getCore() {
        return core;
    }

    public File getBotDataFolder() {
        return botDataFolder;
    }

    public boolean isRunning() {
        return ((CoreImpl) getCore()).isRunning();
    }

    // Note for hardcore developers:
    // You can also put this client into your project as a module to communicate with Kook
    // Call this to start KookBC, then you can use JKook API.
    // WARN: Set the JKook Core by constructing CoreImpl and call JKook.setCore() using it first,
    // or you will get NullPointerException.
    public void start(File file, String token) {
        long timeStamp = System.currentTimeMillis();
        bot = Objects.requireNonNull(loadBot(file, token));
        postLoadBot();
        getCore().getLogger().debug("Starting Network");
        startNetwork();
        finishStart();
        getCore().getLogger().info("Done! ({}s), type \"help\" for help.", TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis() - timeStamp));

        getCore().getScheduler().runTask(new UpdateChecker()); // check update. Added since 2022/7/24
    }

    protected void postLoadBot() {
        getCore().getLogger().debug("Checking the API version of the Bot");
        BotDescription description = bot.getDescription();
        int diff = getVersionDifference(description.getApiVersion(), getCore().getAPIVersion());
        if (diff == -1) {
            getCore().getLogger().warn("Bot is using old version of JKook API! We are using {}, got {}", getCore().getAPIVersion(), description.getApiVersion());
        }
        if (diff == 1) {
            getCore().getLogger().warn("Unsupported API version, we are using {}, got {}", getCore().getAPIVersion(), description.getApiVersion());
        }

        getCore().getLogger().debug("Registering internal things");
        registerInternal();

        getCore().getLogger().debug("Calling Bot#onLoad");
        bot.getLogger().info("Loading " + bot.getDescription().getName() + " version " + bot.getDescription().getVersion());
        bot.onLoad();
        getCore().getLogger().debug("Calling Bot#reloadConfig");
        bot.reloadConfig();
        getCore().getLogger().debug("Calling Bot#onEnable");
        bot.getLogger().info("Enabling " + bot.getDescription().getName() + " version " + bot.getDescription().getVersion());
        bot.onEnable();
        connector = new Connector(this, new NetworkClient(bot));
    }

    protected void startNetwork() {
        connector.start();
    }

    protected void finishStart() {
        User botUser = getEntityBuilder().buildUser(
                connector.getClient().get(HttpAPIRoute.USER_ME.toFullURL())
        );
        getStorage().addUser(botUser);
        bot.setUser(botUser);

        // region BotMarket support part - 2022/7/28
        String rawBotMarketUUID = getConfig().getString("botmarket-uuid");
        if (rawBotMarketUUID != null) {
            UUID bmUUID = null;
            try {
                bmUUID = UUID.fromString(rawBotMarketUUID);
            } catch (IllegalArgumentException ignored) {}
            if (bmUUID != null) {
                new JKookRunnable() {
                    private final Request request =
                            new Request.Builder()
                                    .post(RequestBody.create("", null))
                                    .url("https://bot.gekj.net/api/v1/online.bot")
                                    .header("uuid", rawBotMarketUUID)
                                    .build();
                    
                    @Override
                    public void run() {
                        try {
                            getConnector().getClient().call(request);
                        } catch (Exception e) {
                            JKook.getLogger().error("Unable to PING BotMarket. Your Bot will be marked as OFFLINE in BotMarket.", e);
                        }
                    }
                }.runTaskTimer(TimeUnit.MINUTES.toMillis(30), TimeUnit.MINUTES.toMillis(30));
            }
        }
        // endregion
    }

    // Override this if you have other way to load the Bot.
    protected Bot loadBot(File file, String token) {
        @SuppressWarnings("resource") // we will release it when the client stops. See shutdown() method.
        SimpleBotClassLoader classloader = new SimpleBotClassLoader(this);
        return classloader.loadBot(file, token);
    }

    // If you need console (normally you won't need it), call this
    // Note that this method won't return until the client stopped,
    // so call it in a single thread.
    public void loop() {
        getCore().getLogger().debug("Starting console");
        try {
            new Console(this).start();
        } catch (Exception e) {
            JKook.getLogger().error("Unexpected situation happened during the main loop.", e);
        }
        getCore().getLogger().debug("REPL end");
    }

    // Shutdown this client, and loop() method will return after this method completes.
    public void shutdown() {
        getCore().getLogger().debug("Client shutdown request received");
        if (!((CoreImpl) getCore()).isRunning()) {
            getCore().getLogger().debug("The client has already stopped");
            return;
        }

        getCore().getLogger().info("Stopping client");
        if (bot != null) {
            bot.getLogger().info("Disabling " + bot.getDescription().getName() + " version " + bot.getDescription().getVersion());
            bot.onDisable();
            // why do I check this? because in some environments,
            // the bot won't be loaded by using SimpleClassLoader, maybe another type?
            // And the Bot can be constructed without any check by BotClassLoader,
            // so we should check this before casting it.
            if (bot.getClass().getClassLoader() instanceof SimpleBotClassLoader) {
                try {
                    ((SimpleBotClassLoader) bot.getClass().getClassLoader()).close();
                } catch (IOException e) {
                    JKook.getLogger().error("Unexpected IOException while we attempting to close the Bot ClassLoader.", e);
                }
            }
        }

        if (connector != null) {
            connector.shutdown();
        }
        getCore().shutdown();
        getCore().getLogger().info("Client stopped");
    }

    public Bot getBot() {
        return bot;
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

    protected void registerInternal() {
        new JKookCommand("stop")
                .setDescription("停止 KookBC 实例。")
                .setExecutor(wrapConsoleCmd((args) -> shutdown()))
                .register();
        registerHelpCommand();
        JKook.getEventManager().registerHandlers(new InternalEventListener());
    }

    protected void registerHelpCommand() {
        new JKookCommand("help")
                .setDescription("获取此帮助列表。")
                .setExecutor(
                        (commandSender, args, message) -> {
                            JKookCommand[] result;
                            if (args.length != 0) {
                                String helpWanted = args[0];
                                JKookCommand command = ((CommandManagerImpl) getCore().getCommandManager()).getCommand(helpWanted);
                                if (command == null) {
                                    getCore().getLogger().info("Unknown command.");
                                    return;
                                }
                                result = new JKookCommand[]{command};
                            } else {
                                result = ((CommandManagerImpl) getCore().getCommandManager()).getCommands().toArray(new JKookCommand[0]);
                            }

                            List<String> helpList = getHelp(result);

                            if (commandSender instanceof ConsoleCommandSender) {
                                for (String s : helpList) {
                                    getCore().getLogger().info(s);
                                }
                            } else if (commandSender instanceof User) {
                                helpList.remove(1); // remove /stop help

                                if (getConfig().getBoolean("allow-help-ad", true)) {
                                    helpList.add("由 [KookBC](https://github.com/SNWCreations/KookBC) v" + JKook.getImplementationVersion() + " 驱动 - JKook API " + JKook.getAPIVersion());
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
                .register();
    }

    public static List<String> getHelp(JKookCommand[] commands) {
        List<String> result = new LinkedList<>();
        result.add("-------- 命令帮助 --------");
        for (JKookCommand command : commands) {
            result.add(String.format("(%s)%s: %s", String.join("|", command.getPrefixes()), command.getRootName(),
                    (command.getDescription() == null) ? "此命令没有简介。" : command.getDescription()
            ));
        }
        result.add("注: 在每条命令帮助的开头，括号中用 \"|\" 隔开的字符为此命令的前缀。");
        result.add("如 \"(/|.)blah\" 即 \"/blah\", \".blah\" 为同一条命令。");
        result.add("-------------------------");
        return result;
    }
}
