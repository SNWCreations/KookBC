/*
 *     KookBC -- The Kook Bot Client & JKook API standard implementation for Java.
 *     Copyright (C) 2022 - 2023 KookBC contributors
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

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import snw.jkook.Core;
import snw.jkook.command.CommandExecutor;
import snw.jkook.command.CommandManager;
import snw.jkook.command.JKookCommand;
import snw.jkook.config.ConfigurationSection;
import snw.jkook.entity.User;
import snw.jkook.plugin.InvalidPluginException;
import snw.jkook.plugin.Plugin;
import snw.jkook.plugin.PluginDescription;
import snw.jkook.plugin.UnknownDependencyException;
import snw.jkook.util.Validate;
import snw.kookbc.SharedConstants;
import snw.kookbc.impl.command.CommandManagerImpl;
import snw.kookbc.impl.command.internal.HelpCommand;
import snw.kookbc.impl.command.litecommands.LiteKookFactory;
import snw.kookbc.impl.command.litecommands.internal.PluginsCommand;
import snw.kookbc.impl.console.Console;
import snw.kookbc.impl.entity.builder.EntityBuilder;
import snw.kookbc.impl.entity.builder.MessageBuilder;
import snw.kookbc.impl.event.EventFactory;
import snw.kookbc.impl.event.internal.UserClickButtonListener;
import snw.kookbc.impl.network.HttpAPIRoute;
import snw.kookbc.impl.network.NetworkClient;
import snw.kookbc.impl.network.Session;
import snw.kookbc.impl.network.webhook.JLHttpWebhookNetworkSystem;
import snw.kookbc.impl.network.ws.OkhttpWebSocketNetworkSystem;
import snw.kookbc.impl.plugin.InternalPlugin;
import snw.kookbc.impl.plugin.SimplePluginManager;
import snw.kookbc.impl.scheduler.SchedulerImpl;
import snw.kookbc.impl.storage.EntityStorage;
import snw.kookbc.impl.tasks.BotMarketPingThread;
import snw.kookbc.impl.tasks.StopSignalListener;
import snw.kookbc.impl.tasks.UpdateChecker;
import snw.kookbc.interfaces.network.NetworkSystem;
import snw.kookbc.util.ReturnNotNullFunction;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;

import static snw.kookbc.util.Util.closeLoaderIfPossible;

// The client representation.
public class KBCClient {
    private volatile boolean running = true;
    private final CoreImpl core;
    private final CommandManager commandManager;
    private final NetworkClient networkClient;
    private final EntityStorage storage;
    private final EntityBuilder entityBuilder;
    private final MessageBuilder msgBuilder;
    private final EventFactory eventFactory;
    private final ConfigurationSection config;
    private final File pluginsFolder;
    private final Session session = new Session(null);
    private final InternalPlugin internalPlugin;
    private final ReentrantLock shutdownLock;
    private final Condition shutdownCondition;

    protected final ExecutorService eventExecutor;
    protected final NetworkSystem networkSystem;
    protected List<Plugin> plugins;

    // Use the second one instead, that's recommended
    // However, if you have already specified the network mode in the config object, feel free to use this one
    public KBCClient(CoreImpl core, ConfigurationSection config, File pluginsFolder, String token) {
        this(core, config, pluginsFolder, token, null, null, null, null, null, null, null);
    }

    // The values below is acceptable by the networkMode argument:
    // "websocket", "webhook"
    // (They're same as the "mode" configuration item in kbc.yml!)
    public KBCClient(CoreImpl core, ConfigurationSection config, File pluginsFolder, String token, @NotNull String networkMode) {
        this(core, editNetworkMode(config, networkMode), pluginsFolder, token, null, null, null, null, null, null, null);
    }

    // any other method calls can't be performed before the init method got called, so we have to wrap the edit code...
    // just like ClassLoader() constructor (that constructor use a private constructor which accepts Void, and call a
    //   check method to ensure you have permission to create class loader)
    private static ConfigurationSection editNetworkMode(ConfigurationSection config, String networkMode) {
        config.set("mode", networkMode);
        return config;
    }

    public KBCClient(
            CoreImpl core, ConfigurationSection config, File pluginsFolder, String token,
            /* Customizable components are following: */
            @Nullable ReturnNotNullFunction<KBCClient, CommandManager> commandManager,
            @Nullable ReturnNotNullFunction<KBCClient, NetworkClient> networkClient,
            @Nullable ReturnNotNullFunction<KBCClient, EntityStorage> storage,
            @Nullable ReturnNotNullFunction<KBCClient, EntityBuilder> entityBuilder,
            @Nullable ReturnNotNullFunction<KBCClient, MessageBuilder> msgBuilder,
            @Nullable ReturnNotNullFunction<KBCClient, EventFactory> eventFactory,
            @Nullable ReturnNotNullFunction<KBCClient, NetworkSystem> networkSystem
    ) {
        if (pluginsFolder != null) {
            Validate.isTrue(pluginsFolder.isDirectory(), "The provided pluginsFolder object is not a directory.");
        }
        this.core = core;
        this.config = config;
        this.pluginsFolder = pluginsFolder;
        this.internalPlugin = new InternalPlugin(this);
        this.core.init(this);
        /*Cloud*/
        this.commandManager = Optional.ofNullable(commandManager).orElseGet(() -> CommandManagerImpl::new).apply(this);
        this.networkClient = Optional.ofNullable(networkClient).orElseGet(() -> c -> new NetworkClient(c, token)).apply(this);
        this.storage = Optional.ofNullable(storage).orElseGet(() -> EntityStorage::new).apply(this);
        this.entityBuilder = Optional.ofNullable(entityBuilder).orElseGet(() -> EntityBuilder::new).apply(this);
        this.msgBuilder = Optional.ofNullable(msgBuilder).orElseGet(() -> MessageBuilder::new).apply(this);
        this.eventExecutor = Executors.newSingleThreadExecutor(r -> new Thread(r, "Event Executor"));
        this.shutdownLock = new ReentrantLock();
        this.shutdownCondition = this.shutdownLock.newCondition();
        this.eventFactory = Optional.ofNullable(eventFactory).orElseGet(() -> EventFactory::new).apply(this);
        if (networkSystem == null) {
            final String mode = this.config.getString("mode");
            if ("websocket".equals(mode)) {
                this.networkSystem = new OkhttpWebSocketNetworkSystem(this);
            } else if ("webhook".equals(mode)) {
                this.networkSystem = new JLHttpWebhookNetworkSystem(this, null);
            } else {
                getCore().getLogger().warn("***********************************");
                getCore().getLogger().warn("Unrecognized network mode: " + mode);
                getCore().getLogger().warn("Switch to default mode: websocket");
                getCore().getLogger().warn("***********************************");
                this.networkSystem = new OkhttpWebSocketNetworkSystem(this);
            }
        } else {
            this.networkSystem = networkSystem.apply(this);
        }
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
                if (message != null) {
                    message.sendToSource("你不能这样做，因为你正在尝试执行仅后台可用的命令。");
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
    public synchronized void start() {
        // Print version information
        getCore().getLogger().info("Starting {} version {}", getCore().getImplementationName(), getCore().getImplementationVersion());
        getCore().getLogger().info("This VM is running {} version {} (Implementing API version {})", getCore().getImplementationName(), getCore().getImplementationVersion(), getCore().getAPIVersion());
        getCore().getLogger().info("Working directory: {}", new File(".").getAbsolutePath());
        Properties gitProperties = new Properties();
        try {
            gitProperties.load(getClass().getClassLoader().getResourceAsStream("kookbc_git_data.properties"));
            getCore().getLogger().info("Compiled from Git commit {}, build at {}", gitProperties.get("git.commit.id.full"), gitProperties.get("git.build.time"));
        } catch (NullPointerException | IOException e) {
            getCore().getLogger().warn("Unable to read Git commit information", e);
        }

        if (SharedConstants.IS_SNAPSHOT) {
            getCore().getLogger().warn("***********************************");
            getCore().getLogger().warn("YOU ARE RUNNING A SNAPSHOT BUILD.");
            getCore().getLogger().warn("DO NOT USE SNAPSHOT BUILDS IN YOUR");
            getCore().getLogger().warn(" PRODUCTION ENVIRONMENT!");
            getCore().getLogger().warn("If you don't know why you're seeing");
            getCore().getLogger().warn(" this, download the stable version.");
            getCore().getLogger().warn("***********************************");
        }

        core.getLogger().debug("Fetching Bot user object");
        User botUser = getEntityBuilder().buildUser(
                getNetworkClient().get(HttpAPIRoute.USER_ME.toFullURL()));
        getStorage().addUser(botUser);
        core.setUser(botUser);
        registerInternal();
        getCore().getLogger().debug("Enabling plugins");
        enablePlugins();
        getCore().getLogger().debug("Starting Network");
        startNetwork();
        finishStart();
        getCore().getLogger().info("Done! Type \"help\" for help.");

        if (getConfig().getBoolean("check-update", true)) {
            new UpdateChecker(this).start(); // check update. Added since 2022/7/24
        }
    }

    protected void loadAllPlugins() {
        if (pluginsFolder == null) {
            return; // If you just want to use JKook API?
        }
        if (plugins != null) {
            return;
        }
        List<Plugin> plugins = new LinkedList<>(Arrays.asList(getCore().getPluginManager().loadPlugins(getPluginsFolder())));
        //noinspection ComparatorMethodParameterNotUsed
        plugins.sort(
                (o1, o2) ->
                        (o1.getDescription().getDepend().contains(o2.getDescription().getName())
                                ||
                                o1.getDescription().getSoftDepend().contains(o2.getDescription().getName()))
                                ? 1 : -1
        );

        this.plugins = plugins;
    }

    private void enablePlugins() {
        if (plugins == null || getPluginsFolder() == null) {
            return; // no plugins loaded (or no plugin folder available), so no plugins can be enabled
        }
        @SuppressWarnings("DataFlowIssue")
        List<File> newIncomingFiles = new ArrayList<>(Arrays.asList(getPluginsFolder().listFiles(File::isFile)));

        getCore().getLogger().debug("Before filtering: {}", newIncomingFiles);
        getCore().getLogger().debug("Current known plugins: {}", this.plugins);
        for (Plugin plugin : this.plugins) {
            getCore().getLogger().debug("Checking file: {}", plugin.getFile());
            newIncomingFiles.removeIf(i -> i.getAbsolutePath().equals(plugin.getFile().getAbsolutePath())); // remove already loaded file
        }
        getCore().getLogger().debug("After filtering: {}", newIncomingFiles);

        int before = ((SimplePluginManager) getCore().getPluginManager()).getLoaderProviders().size();

        List<Plugin> pluginsToEnable = this.plugins;
        getCore().getLogger().debug("Plugins to be enabled: {}", pluginsToEnable);

        boolean shouldContinue;

        do {
            shouldContinue = false;
            enablePlugins(pluginsToEnable);
            int after = ((SimplePluginManager) getCore().getPluginManager()).getLoaderProviders().size();
            if (after > before) { // new loader providers added
                getCore().getLogger().debug("Found new plugin loader providers, trying to load more plugins");
                if (!newIncomingFiles.isEmpty()) {
                    getCore().getLogger().debug("Files to be loaded: {}", newIncomingFiles);
                    List<Plugin> newPlugins = new ArrayList<>();
                    for (Iterator<File> iterator = newIncomingFiles.iterator(); iterator.hasNext(); ) {
                        File fileToLoad = iterator.next();
                        final Plugin plugin;
                        try {
                            plugin = getCore().getPluginManager().loadPlugin(fileToLoad);
                        } catch (InvalidPluginException e) {
                            getCore().getLogger().debug("Exception appeared", e);
                            continue; // don't remove, maybe it will be loaded in next loop?
                        }
                        getCore().getLogger().debug("Successfully loaded {} from file {}", plugin, fileToLoad);
                        newPlugins.add(plugin);
                        iterator.remove(); // prevent next loop load this again
                    }
                    getCore().getLogger().debug("New plugins to be enabled in next round: {}", newPlugins);
                    if (!newPlugins.isEmpty()) {
                        pluginsToEnable = newPlugins;
                        shouldContinue = true;
                    }
                }
            }
            before = after;
        } while (shouldContinue);
    }

    private void enablePlugins(@Nullable List<Plugin> plugins) {
        if (plugins == null) { // no plugins? do nothing!
            // if the plugins was not loaded, we can't continue
            // the loadPlugins method is protected, NOT private, so it is possible to be empty!
            return;
        }

        // we must call onLoad() first.
        for (Iterator<Plugin> iterator = plugins.iterator(); iterator.hasNext(); ) {
            Plugin plugin = iterator.next();

            // onLoad
            PluginDescription description = plugin.getDescription();
            plugin.getLogger().info("Loading {} version {}", description.getName(), description.getVersion());
            try {
                plugin.onLoad();
            } catch (Throwable e) {
                plugin.getLogger().error("Unable to load this plugin", e);
                iterator.remove();
            }
            // end onLoad
        }

        for (Iterator<Plugin> iterator = plugins.iterator(); iterator.hasNext(); ) {
            Plugin plugin = iterator.next();

            try {
                plugin.reloadConfig(); // ensure the default configuration will be loaded
            } catch (Exception e) {
                plugin.getLogger().error("Unable to load configuration", e);
            }

            // onEnable
            try {
                getCore().getPluginManager().enablePlugin(plugin);
            } catch (UnknownDependencyException e) {
                getCore().getLogger().error("Unable to enable plugin {} because unknown dependency detected.", plugin.getDescription().getName(), e);
                closeLoaderIfPossible(plugin);
                iterator.remove();
                continue;
            }
            if (!plugin.isEnabled()) {
                closeLoaderIfPossible(plugin);
                iterator.remove();
            } else {
                // Add the plugin into the known list to ensure the dependency system will work correctly
                getCore().getPluginManager().addPlugin(plugin);
            }
            // end onEnable
        }
    }

    protected void startNetwork() {
        networkSystem.start();
    }

    protected void shutdownNetwork() {
        networkSystem.stop();
    }

    protected void finishStart() {
        // region BotMarket support part - 2022/7/28
        String rawBotMarketUUID = getConfig().getString("botmarket-uuid");
        if (rawBotMarketUUID != null) {
            if (!rawBotMarketUUID.isEmpty()) {
                try {
                    //noinspection ResultOfMethodCallIgnored
                    UUID.fromString(rawBotMarketUUID);
                    new BotMarketPingThread(this, rawBotMarketUUID, () -> getNetworkSystem().isConnected()).start();
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
        } catch (IOException e) {
            getCore().getLogger().error("Failed to read input from console");
            getCore().getLogger().error("Running WITHOUT console!");
            getCore().getLogger().error("You can stop this process by creating a new file named");
            getCore().getLogger().error("KOOKBC_STOP in the working directory of this process.");
            getCore().getLogger().error("Stacktrace is following:");
            e.printStackTrace();
            new StopSignalListener(this).start();
        } catch (Exception e) {
            getCore().getLogger().error("Unexpected situation happened during the main loop.", e);
        }
        getCore().getLogger().debug("REPL end");
    }

    // Shutdown this client, and loop() method will return after this method completes.
    public synchronized void shutdown() {
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

        // region Emit shutdown signal
        shutdownLock.lock();
        try {
            shutdownCondition.signalAll();
        } finally {
            shutdownLock.unlock();
        }
        // endregion
    }

    public void waitUntilShutdown() {
        if (!running) {
            return;
        }
        shutdownLock.lock();
        try {
            while (isRunning()) {
                try {
                    shutdownCondition.await();
                } catch (InterruptedException ignored) {
                    // interrupted, but ignore
                }
            }
        } finally {
            shutdownLock.unlock();
        }
    }

    public InternalPlugin getInternalPlugin() {
        return internalPlugin;
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

    public NetworkClient getNetworkClient() {
        return networkClient;
    }

    public Session getSession() {
        return session;
    }

    public ExecutorService getEventExecutor() {
        return eventExecutor;
    }

    public EventFactory getEventFactory() {
        return eventFactory;
    }

    public NetworkSystem getNetworkSystem() {
        return networkSystem;
    }

    protected void registerInternal() {
        ConfigurationSection commandConfig = getConfig().getConfigurationSection("internal-commands");
        if (commandConfig == null) {
            commandConfig = getConfig().createSection("internal-commands");
        }
        if (commandConfig.getBoolean("stop", true)) {
            registerStopCommand();
        }
        if (commandConfig.getBoolean("help", true)) {
            registerHelpCommand();
        }
        if (commandConfig.getBoolean("plugins", true)) {
            registerPluginsCommand();
        }
    }

    protected void registerStopCommand() {
        new JKookCommand("stop")
                .setDescription("停止 " + SharedConstants.IMPL_NAME + " 实例。")
                .setExecutor(wrapConsoleCmd((args) -> shutdown()))
                .register(getInternalPlugin());
    }

    protected void registerPluginsCommand() {
//        new JKookCommand("plugins")
//                .setDescription("获取已安装到此 " + SharedConstants.IMPL_NAME + " 实例的插件列表。")
//                .setExecutor(new PluginsCommand(this))
//                .register(getInternalPlugin());
        LiteKookFactory.builder(getInternalPlugin())
                .commands(new PluginsCommand(this))
                .build()
        ;
    }

    protected void registerHelpCommand() {
        HelpCommand executor = new HelpCommand(this);
        new JKookCommand("help")
                .setDescription("获取此帮助列表。")
                .executesUser(executor)
                .executesConsole(executor)
                .register(getInternalPlugin());
        this.core.getEventManager()
                .registerHandlers(this.internalPlugin, new UserClickButtonListener(this));
    }

    public CommandManager getCommandManager() {
        return commandManager;
    }
}
