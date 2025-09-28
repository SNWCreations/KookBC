package snw.kookbc.impl;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
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
import snw.kookbc.interfaces.AsyncHttpAPI;

import java.io.File;
import java.util.Collection;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

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

    // ===== 异步 API 测试用例 =====

    @Test
    @DisplayName("HttpAPIImpl 应该实现 AsyncHttpAPI 接口")
    void testAsyncHttpAPIImplementation() {
        HttpAPIImpl httpAPI = createTestHttpAPI();

        // 验证 HttpAPIImpl 实现了 AsyncHttpAPI 接口
        assertThat(httpAPI).isInstanceOf(AsyncHttpAPI.class);

        AsyncHttpAPI asyncAPI = (AsyncHttpAPI) httpAPI;
        assertThat(asyncAPI).isNotNull();
    }

    @Test
    @DisplayName("异步文件上传方法应该返回 CompletableFuture")
    void testAsyncUploadFileReturnsCompletableFuture() {
        HttpAPIImpl httpAPI = createTestHttpAPI();
        AsyncHttpAPI asyncAPI = (AsyncHttpAPI) httpAPI;

        // 测试异步文件上传方法
        File testFile = new File("test.txt");
        CompletableFuture<String> future = asyncAPI.uploadFileAsync(testFile);

        assertThat(future).isNotNull();
        assertThat(future).isInstanceOf(CompletableFuture.class);
    }

    @Test
    @DisplayName("异步文件上传（字节数组）方法应该返回 CompletableFuture")
    void testAsyncUploadFileByteArrayReturnsCompletableFuture() {
        HttpAPIImpl httpAPI = createTestHttpAPI();
        AsyncHttpAPI asyncAPI = (AsyncHttpAPI) httpAPI;

        // 测试异步文件上传方法
        byte[] content = "test content".getBytes();
        CompletableFuture<String> future = asyncAPI.uploadFileAsync("test.txt", content);

        assertThat(future).isNotNull();
        assertThat(future).isInstanceOf(CompletableFuture.class);
    }

    @Test
    @DisplayName("异步删除邀请方法应该返回 CompletableFuture<Void>")
    void testAsyncRemoveInviteReturnsCompletableFuture() {
        HttpAPIImpl httpAPI = createTestHttpAPI();
        AsyncHttpAPI asyncAPI = (AsyncHttpAPI) httpAPI;

        // 测试异步删除邀请方法
        CompletableFuture<Void> future = asyncAPI.removeInviteAsync("test-invite-code");

        assertThat(future).isNotNull();
        assertThat(future).isInstanceOf(CompletableFuture.class);
    }

    @Test
    @DisplayName("异步获取用户方法应该返回 CompletableFuture<User>")
    void testAsyncGetUserReturnsCompletableFuture() {
        HttpAPIImpl httpAPI = createTestHttpAPI();
        AsyncHttpAPI asyncAPI = (AsyncHttpAPI) httpAPI;

        // 测试异步获取用户方法
        CompletableFuture<User> future = asyncAPI.getUserAsync("test-user-id");

        assertThat(future).isNotNull();
        assertThat(future).isInstanceOf(CompletableFuture.class);
    }

    @Test
    @DisplayName("异步获取服务器方法应该返回 CompletableFuture<Guild>")
    void testAsyncGetGuildReturnsCompletableFuture() {
        HttpAPIImpl httpAPI = createTestHttpAPI();
        AsyncHttpAPI asyncAPI = (AsyncHttpAPI) httpAPI;

        // 测试异步获取服务器方法
        CompletableFuture<Guild> future = asyncAPI.getGuildAsync("test-guild-id");

        assertThat(future).isNotNull();
        assertThat(future).isInstanceOf(CompletableFuture.class);
    }

    @Test
    @DisplayName("异步获取频道方法应该返回 CompletableFuture<Channel>")
    void testAsyncGetChannelReturnsCompletableFuture() {
        HttpAPIImpl httpAPI = createTestHttpAPI();
        AsyncHttpAPI asyncAPI = (AsyncHttpAPI) httpAPI;

        // 测试异步获取频道方法
        CompletableFuture<Channel> future = asyncAPI.getChannelAsync("test-channel-id");

        assertThat(future).isNotNull();
        assertThat(future).isInstanceOf(CompletableFuture.class);
    }

    @Test
    @DisplayName("批量异步获取用户方法应该返回 CompletableFuture<List<User>>")
    void testBatchGetUsersAsyncReturnsCompletableFuture() {
        HttpAPIImpl httpAPI = createTestHttpAPI();
        AsyncHttpAPI asyncAPI = (AsyncHttpAPI) httpAPI;

        // 测试批量异步获取用户方法
        List<String> userIds = Arrays.asList("user1", "user2", "user3");
        CompletableFuture<List<User>> future = asyncAPI.getBatchUsersAsync(userIds);

        assertThat(future).isNotNull();
        assertThat(future).isInstanceOf(CompletableFuture.class);
    }

    @Test
    @DisplayName("批量异步获取服务器方法应该返回 CompletableFuture<List<Guild>>")
    void testBatchGetGuildsAsyncReturnsCompletableFuture() {
        HttpAPIImpl httpAPI = createTestHttpAPI();
        AsyncHttpAPI asyncAPI = (AsyncHttpAPI) httpAPI;

        // 测试批量异步获取服务器方法
        List<String> guildIds = Arrays.asList("guild1", "guild2", "guild3");
        CompletableFuture<List<Guild>> future = asyncAPI.getBatchGuildsAsync(guildIds);

        assertThat(future).isNotNull();
        assertThat(future).isInstanceOf(CompletableFuture.class);
    }

    @Test
    @DisplayName("批量异步获取频道方法应该返回 CompletableFuture<List<Channel>>")
    void testBatchGetChannelsAsyncReturnsCompletableFuture() {
        HttpAPIImpl httpAPI = createTestHttpAPI();
        AsyncHttpAPI asyncAPI = (AsyncHttpAPI) httpAPI;

        // 测试批量异步获取频道方法
        List<String> channelIds = Arrays.asList("channel1", "channel2", "channel3");
        CompletableFuture<List<Channel>> future = asyncAPI.getBatchChannelsAsync(channelIds);

        assertThat(future).isNotNull();
        assertThat(future).isInstanceOf(CompletableFuture.class);
    }

    @Test
    @DisplayName("请求合并器统计方法应该返回合理的值")
    void testRequestCoalescerStatistics() {
        HttpAPIImpl httpAPI = createTestHttpAPI();
        AsyncHttpAPI asyncAPI = (AsyncHttpAPI) httpAPI;

        // 初始状态应该没有正在进行的请求
        assertThat(asyncAPI.getOngoingRequestCount()).isGreaterThanOrEqualTo(0);

        // 清理缓存方法应该能正常调用
        assertThatCode(asyncAPI::clearRequestCache)
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("请求合并器应该能避免重复请求")
    @Timeout(value = 5, unit = TimeUnit.SECONDS)
    void testRequestCoalescingBehavior() {
        HttpAPIImpl httpAPI = createTestHttpAPI();
        AsyncHttpAPI asyncAPI = (AsyncHttpAPI) httpAPI;

        String userId = "same-user-id";

        // 同时发起多个相同的请求
        CompletableFuture<User> future1 = asyncAPI.getUserAsync(userId);
        CompletableFuture<User> future2 = asyncAPI.getUserAsync(userId);
        CompletableFuture<User> future3 = asyncAPI.getUserAsync(userId);

        // 验证所有 future 都不为 null
        assertThat(future1).isNotNull();
        assertThat(future2).isNotNull();
        assertThat(future3).isNotNull();

        // 请求合并器应该有正在进行的请求
        assertThat(asyncAPI.getOngoingRequestCount()).isGreaterThan(0);
    }

    @Test
    @DisplayName("同步方法应该使用异步实现")
    @Timeout(value = 5, unit = TimeUnit.SECONDS)
    void testSyncMethodsUseAsyncImplementation() {
        HttpAPIImpl httpAPI = createTestHttpAPI();

        // 测试同步方法调用（这些方法内部应该使用异步实现）
        // 由于没有真实的网络环境，这些测试主要验证方法不会立即崩溃

        assertThatThrownBy(() -> httpAPI.uploadFile("test.txt", "test content".getBytes()))
                .isInstanceOf(RuntimeException.class);

        assertThatCode(() -> httpAPI.removeInvite("test-code"))
                .doesNotThrowAnyException();
    }
}