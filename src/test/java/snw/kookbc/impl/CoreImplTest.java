package snw.kookbc.impl;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.helpers.NOPLogger;
import snw.jkook.config.file.YamlConfiguration;
import snw.jkook.entity.User;
import snw.kookbc.SharedConstants;
import snw.kookbc.impl.event.EventManagerImpl;
import snw.kookbc.impl.plugin.SimplePluginManager;
import snw.kookbc.impl.scheduler.SchedulerImpl;

import static org.assertj.core.api.Assertions.*;

@DisplayName("CoreImpl - JKook Core 接口实现测试")
class CoreImplTest {

    private CoreImpl createInitializedCore(String token) {
        CoreImpl core = new CoreImpl(NOPLogger.NOP_LOGGER);
        YamlConfiguration config = new YamlConfiguration();
        // KBCClient 构造函数会自动调用 core.init(this)
        new KBCClient(core, config, null, token);
        return core;
    }

    @Test
    @DisplayName("默认构造函数应该使用 NOP Logger")
    void testDefaultConstructor() {
        CoreImpl coreWithDefaultLogger = new CoreImpl();
        assertThat(coreWithDefaultLogger.getLogger()).isInstanceOf(NOPLogger.class);
    }

    @Test
    @DisplayName("有参构造函数应该正确设置 Logger")
    void testConstructorWithLogger() {
        Logger logger = NOPLogger.NOP_LOGGER;
        CoreImpl coreWithLogger = new CoreImpl(logger);
        assertThat(coreWithLogger.getLogger()).isSameAs(logger);
    }

    @Test
    @DisplayName("getAPIVersion 应该返回正确的 JKook API 版本")
    void testGetAPIVersion() {
        CoreImpl core = createInitializedCore("test-token-api-version");
        assertThat(core.getAPIVersion())
                .isEqualTo(SharedConstants.SPEC_VERSION)
                .matches("\\d+\\.\\d+\\.\\d+(-.+)?");
    }

    @Test
    @DisplayName("getImplementationName 应该返回正确的实现名称")
    void testGetImplementationName() {
        CoreImpl core = createInitializedCore("test-token-impl-name");
        assertThat(core.getImplementationName())
                .isEqualTo(SharedConstants.IMPL_NAME)
                .isEqualTo("KookBC");
    }

    @Test
    @DisplayName("getImplementationVersion 应该返回正确的实现版本")
    void testGetImplementationVersion() {
        CoreImpl core = createInitializedCore("test-token-impl-version");
        assertThat(core.getImplementationVersion())
                .isEqualTo(SharedConstants.IMPL_VERSION)
                .isNotEmpty();
    }

    @Test
    @DisplayName("getHttpAPI 应该返回非 null 的 HttpAPI 实例")
    void testGetHttpAPI() {
        CoreImpl core = createInitializedCore("test-token-http-api");
        assertThat(core.getHttpAPI())
                .isNotNull()
                .isInstanceOf(HttpAPIImpl.class);
    }

    @Test
    @DisplayName("getScheduler 应该返回非 null 的 Scheduler 实例")
    void testGetScheduler() {
        CoreImpl core = createInitializedCore("test-token-scheduler");
        assertThat(core.getScheduler())
                .isNotNull()
                .isInstanceOf(SchedulerImpl.class);
    }

    @Test
    @DisplayName("getEventManager 应该返回非 null 的 EventManager 实例")
    void testGetEventManager() {
        CoreImpl core = createInitializedCore("test-token-event-manager");
        assertThat(core.getEventManager())
                .isNotNull()
                .isInstanceOf(EventManagerImpl.class);
    }

    @Test
    @DisplayName("getPluginManager 应该返回非 null 的 PluginManager 实例")
    void testGetPluginManager() {
        CoreImpl core = createInitializedCore("test-token-plugin-manager");
        assertThat(core.getPluginManager())
                .isNotNull()
                .isInstanceOf(SimplePluginManager.class);
    }

    @Test
    @DisplayName("getUnsafe 应该返回非 null 的 Unsafe 实例")
    void testGetUnsafe() {
        CoreImpl core = createInitializedCore("test-token-unsafe");
        assertThat(core.getUnsafe())
                .isNotNull()
                .isInstanceOf(UnsafeImpl.class);
    }

    @Test
    @DisplayName("getCommandManager 应该返回非 null 的 CommandManager 实例")
    void testGetCommandManager() {
        CoreImpl core = createInitializedCore("test-token-command-manager");
        assertThat(core.getCommandManager())
                .isNotNull();
    }

    @Test
    @DisplayName("getConsoleCommandSender 应该返回非 null 的 ConsoleCommandSender 实例")
    void testGetConsoleCommandSender() {
        CoreImpl core = createInitializedCore("test-token-console-sender");
        assertThat(core.getConsoleCommandSender())
                .isNotNull();
    }

    @Test
    @DisplayName("getClient 应该返回正确的 KBCClient 实例")
    void testGetClient() {
        CoreImpl core = createInitializedCore("test-token-get-client");
        assertThat(core.getClient())
                .isNotNull();
    }

    @Test
    @DisplayName("getUser 初始应该返回 null")
    void testGetUserInitiallyNull() {
        CoreImpl freshCore = new CoreImpl();
        assertThat(freshCore.getUser()).isNull();
    }

    @Test
    @DisplayName("setUser 应该正确设置 Bot 用户")
    void testSetUser() {
        CoreImpl freshCore = createInitializedCore("test-token-setuser");

        // 创建模拟用户 JSON 数据
        com.google.gson.JsonObject userJson = new com.google.gson.JsonObject();
        userJson.addProperty("id", "test-user-id");
        userJson.addProperty("username", "TestUser");
        userJson.addProperty("identify_num", "1234");
        userJson.addProperty("avatar", "https://example.com/avatar.png");
        userJson.addProperty("bot", false); // EntityBuilder.buildUser 需要这个字段
        userJson.addProperty("status", 0); // EntityBuilder.buildUser 需要这个字段来判断用户状态
        userJson.addProperty("is_vip", false); // EntityBuilder.buildUser 需要这个字段
        userJson.addProperty("vip_avatar", "https://example.com/vip_avatar.png"); // EntityBuilder.buildUser 需要这个字段

        User mockUser = freshCore.getClient().getStorage().getUser("test-user-id", userJson);

        freshCore.setUser(mockUser);

        assertThat(freshCore.getUser())
                .isNotNull()
                .isSameAs(mockUser);
    }

    @Test
    @DisplayName("setUser 重复调用应该抛出 IllegalStateException")
    void testSetUserTwiceThrowsException() {
        CoreImpl freshCore = createInitializedCore("test-token-setuser-twice");

        // 创建模拟用户 JSON 数据
        com.google.gson.JsonObject userJson1 = new com.google.gson.JsonObject();
        userJson1.addProperty("id", "test-user-1");
        userJson1.addProperty("username", "User1");
        userJson1.addProperty("identify_num", "1111");
        userJson1.addProperty("avatar", "https://example.com/avatar1.png");
        userJson1.addProperty("bot", false); // EntityBuilder.buildUser 需要这个字段
        userJson1.addProperty("status", 0); // EntityBuilder.buildUser 需要这个字段来判断用户状态
        userJson1.addProperty("is_vip", false); // EntityBuilder.buildUser 需要这个字段
        userJson1.addProperty("vip_avatar", "https://example.com/vip_avatar1.png"); // EntityBuilder.buildUser 需要这个字段

        com.google.gson.JsonObject userJson2 = new com.google.gson.JsonObject();
        userJson2.addProperty("id", "test-user-2");
        userJson2.addProperty("username", "User2");
        userJson2.addProperty("identify_num", "2222");
        userJson2.addProperty("avatar", "https://example.com/avatar2.png");
        userJson2.addProperty("bot", false); // EntityBuilder.buildUser 需要这个字段
        userJson2.addProperty("status", 0); // EntityBuilder.buildUser 需要这个字段来判断用户状态
        userJson2.addProperty("is_vip", false); // EntityBuilder.buildUser 需要这个字段
        userJson2.addProperty("vip_avatar", "https://example.com/vip_avatar2.png"); // EntityBuilder.buildUser 需要这个字段

        User mockUser1 = freshCore.getClient().getStorage().getUser("test-user-1", userJson1);
        User mockUser2 = freshCore.getClient().getStorage().getUser("test-user-2", userJson2);

        freshCore.setUser(mockUser1);

        assertThatThrownBy(() -> freshCore.setUser(mockUser2))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("A user has already bound to this core implementation");
    }

    @Test
    @DisplayName("init 方法应该正确初始化所有组件")
    void testInitializesAllComponents() {
        CoreImpl freshCore = new CoreImpl();
        YamlConfiguration freshConfig = new YamlConfiguration();
        // KBCClient 构造函数会自动调用 core.init()
        new KBCClient(freshCore, freshConfig, null, "test-token-init");

        assertThat(freshCore.getHttpAPI()).isNotNull();
        assertThat(freshCore.getScheduler()).isNotNull();
        assertThat(freshCore.getEventManager()).isNotNull();
        assertThat(freshCore.getPluginManager()).isNotNull();
        assertThat(freshCore.getUnsafe()).isNotNull();
        assertThat(freshCore.getClient()).isNotNull();
    }

    @Test
    @DisplayName("init 方法重复调用应该抛出 IllegalArgumentException")
    void testInitTwiceThrowsException() {
        CoreImpl freshCore = new CoreImpl();
        YamlConfiguration freshConfig1 = new YamlConfiguration();
        // 第一次初始化（通过 KBCClient 构造函数）
        KBCClient freshClient1 = new KBCClient(freshCore, freshConfig1, null, "test-token-init-1");

        // 尝试手动再次初始化同一个 core（这应该抛出异常）
        assertThatThrownBy(() -> freshCore.init(freshClient1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("This core implementation has already initialized");
    }

    @Test
    @DisplayName("init 方法传入 null client 应该抛出异常")
    void testInitWithNullClientThrowsException() {
        CoreImpl freshCore = new CoreImpl();

        assertThatThrownBy(() -> freshCore.init(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("The validated object is null");
    }

    @Test
    @DisplayName("shutdown 应该启动关闭线程")
    void testShutdown() {
        CoreImpl core = createInitializedCore("test-token-shutdown");

        assertThatCode(() -> {
            Thread shutdownThread = new Thread(() -> {
                core.shutdown();
            });
            shutdownThread.start();
            shutdownThread.join(1000);
        }).doesNotThrowAnyException();
    }
}