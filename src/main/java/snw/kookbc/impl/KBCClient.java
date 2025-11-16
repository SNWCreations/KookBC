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
import snw.jkook.config.ConfigurationSection;
import snw.jkook.entity.User;
import snw.jkook.plugin.InvalidPluginException;
import snw.jkook.plugin.Plugin;
import snw.jkook.plugin.PluginDescription;
import snw.jkook.plugin.UnknownDependencyException;
import snw.jkook.util.Validate;
import snw.kookbc.SharedConstants;
import snw.kookbc.impl.command.CommandManagerImpl;
import snw.kookbc.impl.command.litecommands.LiteKookFactory;
import snw.kookbc.impl.command.litecommands.internal.HelpCommand;
import snw.kookbc.impl.command.litecommands.internal.PluginsCommand;
import snw.kookbc.impl.command.litecommands.internal.StopCommand;
import snw.kookbc.impl.command.litecommands.result.ResultTypes;
import snw.kookbc.impl.console.Console;
import snw.kookbc.impl.entity.builder.EntityBuilder;
import snw.kookbc.impl.entity.builder.MessageBuilder;
import snw.kookbc.impl.event.EventFactory;
import snw.kookbc.impl.event.internal.InternalListener;
import snw.kookbc.impl.event.internal.UserClickButtonListener;
import snw.kookbc.impl.network.HttpAPIRoute;
import snw.kookbc.impl.network.NetworkClient;
import snw.kookbc.impl.network.Session;
import snw.kookbc.impl.network.webhook.JLHttpWebhookNetworkSystem;
import snw.kookbc.impl.network.ws.OkhttpWebSocketNetworkSystem;
import snw.kookbc.impl.permissions.UserPermissionSaved;
import snw.kookbc.impl.plugin.InternalPlugin;
import snw.kookbc.impl.plugin.SimplePluginManager;
import snw.kookbc.impl.scheduler.SchedulerImpl;
import snw.kookbc.impl.storage.EntityStorage;
import snw.kookbc.impl.tasks.StopSignalListener;
import snw.kookbc.impl.tasks.UpdateChecker;
import snw.kookbc.interfaces.network.NetworkSystem;
import snw.kookbc.util.DependencyListBasedPluginComparator;
import snw.kookbc.util.ReturnNotNullFunction;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;

import static snw.kookbc.util.Util.closeLoaderIfPossible;
import static snw.kookbc.util.VirtualThreadUtil.newVirtualThreadExecutor;

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
    private final Thread mainThread = Thread.currentThread();

    protected final ExecutorService eventExecutor;
    protected final NetworkSystem networkSystem;
    protected List<Plugin> plugins;
    protected final Map<String, UserPermissionSaved> userPermissions = new HashMap<>();

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

    public KBCClient(CoreImpl core, ConfigurationSection config, File pluginsFolder, String token,
            /* Customizable components are following: */
                     @Nullable ReturnNotNullFunction<KBCClient, CommandManager> commandManager, @Nullable ReturnNotNullFunction<KBCClient, NetworkClient> networkClient, @Nullable ReturnNotNullFunction<KBCClient, EntityStorage> storage, @Nullable ReturnNotNullFunction<KBCClient, EntityBuilder> entityBuilder, @Nullable ReturnNotNullFunction<KBCClient, MessageBuilder> msgBuilder, @Nullable ReturnNotNullFunction<KBCClient, EventFactory> eventFactory, @Nullable ReturnNotNullFunction<KBCClient, NetworkSystem> networkSystem) {
        if (pluginsFolder != null) {
            Validate.isTrue(pluginsFolder.isDirectory(), "The provided pluginsFolder object is not a directory.");
        }
        this.core = core;
        this.config = config;
        this.pluginsFolder = pluginsFolder;
        this.internalPlugin = new InternalPlugin(this);
        this.core.init(this);
        this.commandManager = Optional.ofNullable(commandManager).orElseGet(() -> CommandManagerImpl::new).apply(this);
        this.networkClient = Optional.ofNullable(networkClient).orElseGet(() -> c -> new NetworkClient(c, token)).apply(this);
        this.storage = Optional.ofNullable(storage).orElseGet(() -> EntityStorage::new).apply(this);
        this.entityBuilder = Optional.ofNullable(entityBuilder).orElseGet(() -> EntityBuilder::new).apply(this);
        this.msgBuilder = Optional.ofNullable(msgBuilder).orElseGet(() -> MessageBuilder::new).apply(this);
        this.eventExecutor = newVirtualThreadExecutor("Event-Executor");
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
                getCore().getLogger().warn("无法识别的网络模式: " + mode);
                getCore().getLogger().warn("切换到默认模式: websocket");
                getCore().getLogger().warn("***********************************");
                this.networkSystem = new OkhttpWebSocketNetworkSystem(this);
            }
        } else {
            this.networkSystem = networkSystem.apply(this);
        }
        loadPermissions();
    }

    protected void loadPermissions() {
        String path = System.getProperty("kookbc.permissions.file", "permissions.json");
        File file = new File(path);
        if (!file.exists()) {
            return;
        }
        try (FileInputStream inputStream = new FileInputStream(file)) {
            byte[] bytes = new byte[inputStream.available()];
            inputStream.read(bytes);
            String json = new String(bytes, StandardCharsets.UTF_8);
            List<UserPermissionSaved> parseList = UserPermissionSaved.parseList(json);
            for (UserPermissionSaved userPermissionSaved : parseList) {
                this.userPermissions.put(userPermissionSaved.getUid(), userPermissionSaved);
            }
        } catch (IOException e) {
            getCore().getLogger().error("加载权限失败", e);
        }
    }

    public void savePermissions() {
        String path = System.getProperty("kookbc.permissions.file", "permissions.json");
        try {
            File file = new File(path);
            if (file.getParentFile()!=null && !file.getParentFile().exists()) {
                file.getParentFile().mkdirs();
            }
            if (!file.exists()) {
                file.createNewFile();
            }
            try (FileOutputStream outputStream = new FileOutputStream(file)) {
                outputStream.write(UserPermissionSaved.toString(this.userPermissions.values().toArray(new UserPermissionSaved[0])).getBytes(StandardCharsets.UTF_8));
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
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
        getCore().getLogger().info("正在启动 {} 版本 {}", getCore().getImplementationName(), getCore().getImplementationVersion());
        getCore().getLogger().info("当前运行 {} 版本 {} (实现 API 版本 {})", getCore().getImplementationName(), getCore().getImplementationVersion(), getCore().getAPIVersion());
        getCore().getLogger().info("工作目录: {}", new File(".").getAbsolutePath());
        Properties gitProperties = new Properties();
        try {
            gitProperties.load(getClass().getClassLoader().getResourceAsStream("kookbc_git_data.properties"));
            getCore().getLogger().info("编译信息: Git 提交 {}, 构建时间 {}", gitProperties.get("git.commit.id"), gitProperties.get("git.commit.time"));
        } catch (NullPointerException | IOException e) {
            getCore().getLogger().warn("无法读取 Git 提交信息", e);
        }

        if (SharedConstants.IS_SNAPSHOT) {
            getCore().getLogger().warn("***********************************");
            getCore().getLogger().warn("您正在运行快照构建版本。");
            getCore().getLogger().warn("请勿在生产环境中使用");
            getCore().getLogger().warn(" 快照构建版本！");
            getCore().getLogger().warn("如果您不知道为什么看到");
            getCore().getLogger().warn(" 这个消息，请下载稳定版本。");
            getCore().getLogger().warn("***********************************");
        }

        core.getLogger().debug("正在获取 Bot 用户对象");
        User botUser = getEntityBuilder().buildUser(getNetworkClient().get(HttpAPIRoute.USER_ME.toFullURL()));
        getStorage().addUser(botUser);
        core.setUser(botUser);
        registerInternal();
        getCore().getLogger().debug("正在启用插件");
        enablePlugins();
        getCore().getLogger().info("正在运行延迟初始化任务");
        ((SchedulerImpl) core.getScheduler()).runAfterPluginInitTasks();
        getCore().getLogger().debug("正在启动网络");
        startNetwork();
        finishStart();
        getCore().getLogger().info("完成！输入 \"help\" 获取帮助。");

        if (getConfig().getBoolean("check-update", true)) {
            new UpdateChecker(this).start(); // check update. Added since 2022/7/24
        }
    }

    protected void loadAllPlugins() {
        final File pluginsFolder = getPluginsFolder();
        if (pluginsFolder == null) {
            return; // If you just want to use JKook API?
        }
        if (plugins != null) {
            return;
        }
        List<Plugin> plugins = new LinkedList<>(Arrays.asList(getCore().getPluginManager().loadPlugins(pluginsFolder)));
        plugins.sort(DependencyListBasedPluginComparator.INSTANCE);

        this.plugins = plugins;
    }

    private void enablePlugins() {
        if (plugins == null || getPluginsFolder() == null) {
            return; // no plugins loaded (or no plugin folder available), so no plugins can be enabled
        }
        @SuppressWarnings("DataFlowIssue") List<File> newIncomingFiles = new ArrayList<>(Arrays.asList(getPluginsFolder().listFiles(File::isFile)));

        getCore().getLogger().debug("过滤前: {}", newIncomingFiles);
        getCore().getLogger().debug("当前已知插件: {}", this.plugins);
        for (Plugin plugin : this.plugins) {
            getCore().getLogger().debug("正在检查文件: {}", plugin.getFile());
            newIncomingFiles.removeIf(i -> i.getAbsolutePath().equals(plugin.getFile().getAbsolutePath())); // remove already loaded file
        }
        getCore().getLogger().debug("过滤后: {}", newIncomingFiles);

        int before = ((SimplePluginManager) getCore().getPluginManager()).getLoaderProviders().size();

        List<Plugin> pluginsToEnable = this.plugins;
        getCore().getLogger().debug("待启用的插件: {}", pluginsToEnable);

        boolean shouldContinue;

        do {
            shouldContinue = false;
            enablePlugins(pluginsToEnable);
            int after = ((SimplePluginManager) getCore().getPluginManager()).getLoaderProviders().size();
            if (after > before) { // new loader providers added
                getCore().getLogger().debug("发现新的插件加载器提供者，尝试加载更多插件");
                if (!newIncomingFiles.isEmpty()) {
                    getCore().getLogger().debug("待加载的文件: {}", newIncomingFiles);
                    List<Plugin> newPlugins = new ArrayList<>();
                    for (Iterator<File> iterator = newIncomingFiles.iterator(); iterator.hasNext(); ) {
                        File fileToLoad = iterator.next();
                        final Plugin plugin;
                        try {
                            plugin = getCore().getPluginManager().loadPlugin(fileToLoad);
                        } catch (InvalidPluginException e) {
                            getCore().getLogger().debug("出现异常", e);
                            continue; // don't remove, maybe it will be loaded in next loop?
                        }
                        getCore().getLogger().debug("成功从文件 {} 加载插件 {}", fileToLoad, plugin);
                        newPlugins.add(plugin);
                        iterator.remove(); // prevent next loop load this again
                    }
                    getCore().getLogger().debug("下一轮待启用的新插件: {}", newPlugins);
                    if (!newPlugins.isEmpty()) {
                        newPlugins.sort(DependencyListBasedPluginComparator.INSTANCE);
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
            plugin.getLogger().info("正在加载 {} 版本 {}", description.getName(), description.getVersion());
            try {
                plugin.onLoad();
            } catch (Throwable e) {
                plugin.getLogger().error("无法加载此插件", e);
                iterator.remove();
            }
            // end onLoad
        }

        for (Iterator<Plugin> iterator = plugins.iterator(); iterator.hasNext(); ) {
            Plugin plugin = iterator.next();

            try {
                plugin.reloadConfig(); // ensure the default configuration will be loaded
            } catch (Exception e) {
                plugin.getLogger().error("无法加载配置", e);
            }

            // onEnable
            try {
                getCore().getPluginManager().enablePlugin(plugin);
            } catch (UnknownDependencyException e) {
                getCore().getLogger().error("无法启用插件 {}，检测到未知的依赖项", plugin.getDescription().getName(), e);
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
                /*
                try {
                    //noinspection ResultOfMethodCallIgnored
                    UUID.fromString(rawBotMarketUUID);
                    new BotMarketPingThread(this, rawBotMarketUUID, () -> getNetworkSystem().isConnected()).start();
                } catch (IllegalArgumentException e) {
                    getCore().getLogger().warn("BotMarket 的 UUID 无效，不会为 BotMarket 安排 PING 任务");
                }
                */
                getCore().getLogger().warn("BotMarket Ping 目前已弃用，他们正在升级系统");
            }
        }
        // endregion
    }

    // If you need console (normally you won't need it), call this
    // Note that this method won't return until the client stopped,
    // so call it in a single thread.
    public void loop() {
        getCore().getLogger().debug("正在启动控制台");
        try {
            new Console(this).start();
        } catch (IOException e) {
            getCore().getLogger().error("从控制台读取输入失败");
            getCore().getLogger().error("在没有控制台的情况下运行！");
            getCore().getLogger().error("您可以通过创建一个名为");
            getCore().getLogger().error("KOOKBC_STOP 的新文件来停止此进程，文件位于此进程的工作目录中");
            getCore().getLogger().error("堆栈跟踪如下：");
            e.printStackTrace();
            new StopSignalListener(this).start();
        } catch (Exception e) {
            getCore().getLogger().error("主循环执行过程中发生意外情况", e);
        }
        getCore().getLogger().debug("REPL 结束");
    }

    // Shutdown this client, and loop() method will return after this method completes.
    public synchronized void shutdown() {
        getCore().getLogger().debug("收到客户端关闭请求");
        if (!isRunning()) {
            getCore().getLogger().debug("客户端已经停止");
            return;
        }
        running = false; // make sure the client will shut down if Bot wish the client stop.

        getCore().getLogger().info("正在停止客户端");
        getCore().getPluginManager().clearPlugins();

        shutdownNetwork();
        eventExecutor.shutdown();
        getCore().getLogger().info("正在停止核心");
        getCore().getLogger().info("正在停止调度器（如果应用程序陷入无限循环，请终止此进程！）");
        ((SchedulerImpl) getCore().getScheduler()).shutdown();
        getCore().getLogger().info("客户端已停止");

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

    public boolean isPrimaryThread() {
        return Thread.currentThread() == mainThread;
    }

    protected void registerInternal() {
        this.core.getEventManager().registerHandlers(this.internalPlugin, new InternalListener());
        ConfigurationSection commandConfig = getConfig().getConfigurationSection("internal-commands");
        if (commandConfig == null) {
            commandConfig = getConfig().createSection("internal-commands");
        }
        List<Class<?>> commands = new ArrayList<>();
        if (commandConfig.getBoolean("stop", true)) {
            commands.add(StopCommand.class);
        }
        if (commandConfig.getBoolean("help", true)) {
            this.core.getEventManager().registerHandlers(this.internalPlugin, new UserClickButtonListener(this));
            commands.add(HelpCommand.class);
        }
        if (commandConfig.getBoolean("plugins", true)) {
            commands.add(PluginsCommand.class);
        }
        registerCommands(commands);
    }

    private void registerCommands(List<Class<?>> commands) {
        LiteKookFactory.builder(getInternalPlugin()).settings((k) -> {
            if (!getConfig().contains("internal-commands-reply-result-type")) {
                return k;
            }
            try {
                ResultTypes resultTypes = ResultTypes.valueOf(getConfig().getString("internal-commands-reply-result-type"));
                k.defaultResultType(resultTypes);
            } catch (Exception e) {
                getCore().getLogger().error("`internal-commands-reply-result-type` 不是有效的 result-type");
            }
            return k;
        }).commands(commands.toArray()).build();
    }

    public CommandManager getCommandManager() {
        return commandManager;
    }

    public Map<String, UserPermissionSaved> getUserPermissions() {
        return userPermissions;
    }
}
