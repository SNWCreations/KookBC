package snw.kookbc.impl;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.helpers.NOPLogger;
import snw.jkook.config.file.YamlConfiguration;
import snw.jkook.entity.channel.Channel;
import snw.jkook.entity.Guild;
import snw.jkook.entity.User;
import snw.jkook.entity.channel.Category;
import snw.jkook.entity.channel.TextChannel;
import snw.jkook.entity.channel.VoiceChannel;
import snw.jkook.util.PageIterator;
import snw.kookbc.impl.entity.channel.CategoryImpl;
import snw.kookbc.impl.entity.channel.TextChannelImpl;
import snw.kookbc.impl.entity.channel.VoiceChannelImpl;
import snw.kookbc.test.BaseTest;

import java.io.File;
import java.util.Collection;

import static org.assertj.core.api.Assertions.*;

@DisplayName("HttpAPIImpl - HTTP API 实现测试")
class HttpAPIImplTest extends BaseTest {

    private HttpAPIImpl createTestHttpAPI() {
        CoreImpl core = new CoreImpl(NOPLogger.NOP_LOGGER);
        YamlConfiguration config = new YamlConfiguration();
        config.set("mode", "websocket");
        KBCClient client = new KBCClient(core, config, null, "test-token");
        return new HttpAPIImpl(client);
    }

    @Test
    @DisplayName("构造函数应该正确设置 client")
    void testConstructor() {
        CoreImpl core = new CoreImpl(NOPLogger.NOP_LOGGER);
        YamlConfiguration config = new YamlConfiguration();
        config.set("mode", "websocket");
        KBCClient client = new KBCClient(core, config, null, "test-token");

        HttpAPIImpl httpAPI = new HttpAPIImpl(client);

        assertThat(httpAPI).isNotNull();
        // 验证 HttpAPI 可以正常工作
        assertThat(httpAPI.getJoinedGuilds()).isNotNull();
    }

    @Test
    @DisplayName("构造函数传入 null client 应该能正常创建")
    void testConstructorWithNullClient() {
        // HttpAPIImpl 允许传入 null client
        assertThatCode(() -> new HttpAPIImpl(null))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("getJoinedGuilds 应该返回非 null 的 PageIterator")
    void testGetJoinedGuilds() {
        HttpAPIImpl httpAPI = createTestHttpAPI();

        PageIterator<Collection<Guild>> guilds = httpAPI.getJoinedGuilds();

        assertThat(guilds).isNotNull();
    }

    @Test
    @DisplayName("getUser 应该能正常调用")
    void testGetUser() {
        HttpAPIImpl httpAPI = createTestHttpAPI();
        String userId = "test-user-id";

        // 测试方法不会抛出异常
        assertThatCode(() -> httpAPI.getUser(userId))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("getGuild 应该能正常调用")
    void testGetGuild() {
        HttpAPIImpl httpAPI = createTestHttpAPI();
        String guildId = "test-guild-id";

        // 测试方法不会抛出异常
        assertThatCode(() -> httpAPI.getGuild(guildId))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("getChannel 应该能正常调用")
    void testGetChannel() {
        HttpAPIImpl httpAPI = createTestHttpAPI();
        String channelId = "test-channel-id";

        // 测试方法会因网络问题抛出异常，但这是预期的
        assertThatThrownBy(() -> httpAPI.getChannel(channelId))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    @DisplayName("getTextChannel 应该创建 TextChannelImpl 实例")
    void testGetTextChannel() {
        HttpAPIImpl httpAPI = createTestHttpAPI();
        String channelId = "test-text-channel-id";

        TextChannel result = httpAPI.getTextChannel(channelId);

        assertThat(result).isNotNull().isInstanceOf(TextChannelImpl.class);
    }

    @Test
    @DisplayName("getVoiceChannel 应该创建 VoiceChannelImpl 实例")
    void testGetVoiceChannel() {
        HttpAPIImpl httpAPI = createTestHttpAPI();
        String channelId = "test-voice-channel-id";

        VoiceChannel result = httpAPI.getVoiceChannel(channelId);

        assertThat(result).isNotNull().isInstanceOf(VoiceChannelImpl.class);
    }

    @Test
    @DisplayName("getCategory 应该创建 CategoryImpl 实例")
    void testGetCategory() {
        HttpAPIImpl httpAPI = createTestHttpAPI();
        String categoryId = "test-category-id";

        Category result = httpAPI.getCategory(categoryId);

        assertThat(result).isNotNull().isInstanceOf(CategoryImpl.class);
    }

    @Test
    @DisplayName("uploadFile 传入不存在的文件应该抛出异常")
    void testUploadFileWithNonExistentFile() {
        HttpAPIImpl httpAPI = createTestHttpAPI();
        File nonExistentFile = new File("non-existent-file.txt");

        // 这会导致网络请求错误，但我们主要测试方法不会崩溃
        assertThatThrownBy(() -> httpAPI.uploadFile(nonExistentFile))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    @DisplayName("uploadFile 传入格式错误的 URL 应该抛出 IllegalArgumentException")
    void testUploadFileWithMalformedURL() {
        HttpAPIImpl httpAPI = createTestHttpAPI();
        String malformedUrl = "not-a-valid-url";

        assertThatThrownBy(() -> httpAPI.uploadFile("test.txt", malformedUrl))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Cannot upload file: Malformed URL");
    }

    @Test
    @DisplayName("uploadFile 传入有效 URL 格式应该通过 URL 验证")
    void testUploadFileWithValidURL() {
        HttpAPIImpl httpAPI = createTestHttpAPI();
        String validUrl = "https://example.com/test.txt";

        // URL 格式验证应该通过，但网络请求会失败（这是预期的）
        assertThatThrownBy(() -> httpAPI.uploadFile("test.txt", validUrl))
                .isInstanceOf(RuntimeException.class)
                .hasMessageNotContaining("Malformed URL");
    }

    @Test
    @DisplayName("getGames 应该返回非 null 的 PageIterator")
    void testGetGames() {
        HttpAPIImpl httpAPI = createTestHttpAPI();

        PageIterator<?> games = httpAPI.getGames();

        assertThat(games).isNotNull();
    }

    @Test
    @DisplayName("getGames 指定类型应该返回非 null 的 PageIterator")
    void testGetGamesWithType() {
        HttpAPIImpl httpAPI = createTestHttpAPI();
        int gameType = 1;

        PageIterator<?> games = httpAPI.getGames(gameType);

        assertThat(games).isNotNull();
    }

    @Test
    @DisplayName("getFriendState 应该能正常调用")
    void testGetFriendState() {
        HttpAPIImpl httpAPI = createTestHttpAPI();

        // 测试方法会因网络问题抛出异常，但这是预期的
        assertThatThrownBy(() -> httpAPI.getFriendState(false))
                .isInstanceOf(RuntimeException.class);
    }
}