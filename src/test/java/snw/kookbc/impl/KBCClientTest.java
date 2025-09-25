package snw.kookbc.impl;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.helpers.NOPLogger;
import snw.jkook.config.file.YamlConfiguration;
import snw.jkook.entity.User;
import snw.kookbc.impl.storage.EntityStorage;
import snw.kookbc.impl.entity.builder.EntityBuilder;
import snw.kookbc.impl.entity.builder.MessageBuilder;
import snw.kookbc.impl.event.EventFactory;
import snw.kookbc.impl.network.NetworkClient;
import snw.kookbc.impl.network.Session;
import snw.kookbc.test.BaseTest;

import java.io.File;

import static org.assertj.core.api.Assertions.*;

@DisplayName("KBCClient - Bot 客户端实现测试")
class KBCClientTest extends BaseTest {

    private KBCClient createTestClient() {
        CoreImpl core = new CoreImpl(NOPLogger.NOP_LOGGER);
        YamlConfiguration config = new YamlConfiguration();
        config.set("mode", "websocket");  // 设置默认网络模式
        return new KBCClient(core, config, null, "test-token");
    }

    @Test
    @DisplayName("构造函数应该正确初始化所有组件")
    void testConstructorInitializesAllComponents() {
        KBCClient client = createTestClient();

        // 验证核心组件
        assertThat(client.getCore()).isNotNull();
        assertThat(client.getConfig()).isNotNull();
        assertThat(client.getPluginsFolder()).isNull(); // 我们传入了 null

        // 验证内部组件
        assertThat(client.getInternalPlugin()).isNotNull();
        assertThat(client.getStorage()).isNotNull().isInstanceOf(EntityStorage.class);
        assertThat(client.getEntityBuilder()).isNotNull().isInstanceOf(EntityBuilder.class);
        assertThat(client.getMessageBuilder()).isNotNull().isInstanceOf(MessageBuilder.class);
        assertThat(client.getNetworkClient()).isNotNull().isInstanceOf(NetworkClient.class);
        assertThat(client.getSession()).isNotNull().isInstanceOf(Session.class);
    }

    @Test
    @DisplayName("构造函数传入 null CoreImpl 应该抛出异常")
    void testConstructorWithNullCoreThrowsException() {
        YamlConfiguration config = new YamlConfiguration();

        assertThatThrownBy(() -> new KBCClient(null, config, null, "test-token"))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("构造函数传入 null 配置应该抛出异常")
    void testConstructorWithNullConfigThrowsException() {
        CoreImpl core = new CoreImpl(NOPLogger.NOP_LOGGER);

        assertThatThrownBy(() -> new KBCClient(core, null, null, "test-token"))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    @DisplayName("构造函数传入 null token 应该能正常创建")
    void testConstructorWithNullToken() {
        CoreImpl core = new CoreImpl(NOPLogger.NOP_LOGGER);
        YamlConfiguration config = new YamlConfiguration();
        config.set("mode", "websocket");

        assertThatCode(() -> {
            KBCClient client = new KBCClient(core, config, null, null);
            assertThat(client).isNotNull();
            assertThat(client.getNetworkClient()).isNotNull();
        }).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("构造函数传入非目录的插件文件夹应该抛出异常")
    void testConstructorWithNonDirectoryPluginsFolder() {
        CoreImpl core = new CoreImpl(NOPLogger.NOP_LOGGER);
        YamlConfiguration config = new YamlConfiguration();
        File notADirectory = new File("nonexistent-file.txt");

        assertThatThrownBy(() -> new KBCClient(core, config, notADirectory, "test-token"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("The provided pluginsFolder object is not a directory");
    }

    @Test
    @DisplayName("getCore 应该返回正确的 Core 实例")
    void testGetCore() {
        CoreImpl core = new CoreImpl(NOPLogger.NOP_LOGGER);
        YamlConfiguration config = new YamlConfiguration();
        config.set("mode", "websocket");
        KBCClient client = new KBCClient(core, config, null, "test-token");

        assertThat(client.getCore()).isSameAs(core);
    }

    @Test
    @DisplayName("getConfig 应该返回正确的配置对象")
    void testGetConfig() {
        CoreImpl core = new CoreImpl(NOPLogger.NOP_LOGGER);
        YamlConfiguration config = new YamlConfiguration();
        config.set("mode", "websocket");
        config.set("test-key", "test-value");
        KBCClient client = new KBCClient(core, config, null, "test-token");

        assertThat(client.getConfig()).isSameAs(config);
        assertThat(client.getConfig().getString("test-key")).isEqualTo("test-value");
    }

    @Test
    @DisplayName("isRunning 初始应该返回 true")
    void testIsRunningInitiallyTrue() {
        KBCClient client = createTestClient();

        assertThat(client.isRunning()).isTrue();
    }

    @Test
    @DisplayName("getSession 应该返回非 null 的 Session 实例")
    void testGetSession() {
        KBCClient client = createTestClient();

        Session session = client.getSession();
        assertThat(session).isNotNull();
        assertThat(session.getId()).isNull(); // 初始 session ID 应该为空
    }

    @Test
    @DisplayName("getStorage 应该返回非 null 的 EntityStorage 实例")
    void testGetStorage() {
        KBCClient client = createTestClient();

        assertThat(client.getStorage()).isNotNull().isInstanceOf(EntityStorage.class);
    }

    @Test
    @DisplayName("getEntityBuilder 应该返回非 null 的 EntityBuilder 实例")
    void testGetEntityBuilder() {
        KBCClient client = createTestClient();

        assertThat(client.getEntityBuilder()).isNotNull().isInstanceOf(EntityBuilder.class);
    }

    @Test
    @DisplayName("getMessageBuilder 应该返回非 null 的 MessageBuilder 实例")
    void testGetMessageBuilder() {
        KBCClient client = createTestClient();

        assertThat(client.getMessageBuilder()).isNotNull().isInstanceOf(MessageBuilder.class);
    }

    @Test
    @DisplayName("getNetworkClient 应该返回非 null 的 NetworkClient 实例")
    void testGetNetworkClient() {
        KBCClient client = createTestClient();

        assertThat(client.getNetworkClient()).isNotNull().isInstanceOf(NetworkClient.class);
    }

    @Test
    @DisplayName("getInternalPlugin 应该返回非 null 的内部插件")
    void testGetInternalPlugin() {
        KBCClient client = createTestClient();

        assertThat(client.getInternalPlugin()).isNotNull();
    }

    @Test
    @DisplayName("构造函数指定 WebSocket 网络模式应该正确设置")
    void testConstructorWithWebSocketMode() {
        CoreImpl core = new CoreImpl(NOPLogger.NOP_LOGGER);
        YamlConfiguration config = new YamlConfiguration();

        // 测试 websocket 模式
        KBCClient wsClient = new KBCClient(core, config, null, "test-token", "websocket");
        assertThat(wsClient.getConfig().getString("mode")).isEqualTo("websocket");
        assertThat(wsClient.getNetworkClient()).isNotNull();
    }

    @Test
    @DisplayName("未知网络模式应该回退到默认的 websocket 模式")
    void testUnknownNetworkModeFallsBackToWebsocket() {
        CoreImpl core = new CoreImpl(NOPLogger.NOP_LOGGER);
        YamlConfiguration config = new YamlConfiguration();
        config.set("mode", "unknown-mode");

        // 这应该不会抛出异常，而是回退到 websocket 模式
        assertThatCode(() -> {
            KBCClient client = new KBCClient(core, config, null, "test-token");
            assertThat(client).isNotNull();
            assertThat(client.getNetworkClient()).isNotNull();
        }).doesNotThrowAnyException();
    }
}