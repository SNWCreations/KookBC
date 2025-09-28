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

package snw.kookbc.interfaces;

import java.io.File;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import snw.jkook.entity.Guild;
import snw.jkook.entity.User;
import snw.jkook.entity.channel.Channel;

/**
 * 异步 HTTP API 接口 - 为插件提供高性能的异步 API 调用能力
 *
 * <p>该接口提供了 KookBC HTTP API 的异步版本，利用 Java 21 虚拟线程技术
 * 实现高并发、低延迟的 API 调用。所有方法都返回 {@link CompletableFuture}，
 * 支持链式调用和组合操作。
 *
 * <p><b>性能优势</b>：
 * <ul>
 *   <li>使用虚拟线程，支持大量并发请求而不阻塞系统</li>
 *   <li>内置智能请求合并，避免重复的并发请求</li>
 *   <li>批量操作 API，显著减少网络调用次数</li>
 *   <li>异步执行，不阻塞主线程或事件处理</li>
 * </ul>
 *
 * <p><b>使用示例</b>：
 * <pre>{@code
 * // 异步文件上传
 * asyncHttpAPI.uploadFileAsync(file)
 *     .thenAccept(url -> {
 *         // 处理上传结果
 *         logger.info("文件上传成功: {}", url);
 *     })
 *     .exceptionally(throwable -> {
 *         // 处理异常
 *         logger.error("文件上传失败", throwable);
 *         return null;
 *     });
 *
 * // 批量获取用户信息
 * List<String> userIds = Arrays.asList("123", "456", "789");
 * asyncHttpAPI.getBatchUsersAsync(userIds)
 *     .thenAccept(users -> {
 *         // 处理批量结果
 *         users.forEach(user ->
 *             logger.info("用户: {}", user.getName()));
 *     });
 *
 * // 组合异步操作
 * CompletableFuture<String> uploadFuture = asyncHttpAPI.uploadFileAsync(file);
 * CompletableFuture<User> userFuture = asyncHttpAPI.getUserAsync("123");
 *
 * CompletableFuture.allOf(uploadFuture, userFuture)
 *     .thenRun(() -> {
 *         String url = uploadFuture.join();
 *         User user = userFuture.join();
 *         // 所有操作都完成
 *     });
 * }</pre>
 *
 * <p><b>错误处理</b>：
 * 所有异步方法在遇到错误时会返回失败的 {@link CompletableFuture}。
 * 建议使用 {@code exceptionally()} 或 {@code handle()} 方法进行错误处理。
 *
 * <p><b>线程安全</b>：
 * 该接口的所有方法都是线程安全的，可以在多线程环境中安全使用。
 *
 * @since KookBC 0.33.0
 * @see snw.jkook.HttpAPI
 * @see java.util.concurrent.CompletableFuture
 */
public interface AsyncHttpAPI {

    // ===== 文件操作 =====

    /**
     * 异步上传文件
     *
     * <p>将指定的文件异步上传到 Kook 服务器，并返回包含文件 URL 的 Future。
     * 支持的文件类型包括图片、音频、视频和其他文档。
     *
     * @param file 要上传的文件，不能为 null
     * @return 异步结果，包含上传后的文件 URL
     * @throws IllegalArgumentException 如果文件为 null 或不存在
     */
    @NotNull
    CompletableFuture<String> uploadFileAsync(@NotNull File file);

    /**
     * 异步上传文件内容
     *
     * <p>将指定的字节数组内容异步上传到 Kook 服务器。
     * 适用于内存中的文件内容或动态生成的内容。
     *
     * @param filename 文件名，用于服务器端识别文件类型
     * @param content 文件内容的字节数组
     * @return 异步结果，包含上传后的文件 URL
     * @throws IllegalArgumentException 如果文件名为空或内容为 null
     */
    @NotNull
    CompletableFuture<String> uploadFileAsync(@NotNull String filename, @NotNull byte[] content);

    /**
     * 异步上传网络文件
     *
     * <p>从指定 URL 下载文件并上传到 Kook 服务器。
     * 适用于转存其他平台的文件资源。
     *
     * @param fileName 目标文件名
     * @param url 源文件的网络地址
     * @return 异步结果，包含上传后的文件 URL
     * @throws IllegalArgumentException 如果 URL 格式错误
     */
    @NotNull
    CompletableFuture<String> uploadFileAsync(@NotNull String fileName, @NotNull String url);

    // ===== 邀请管理 =====

    /**
     * 异步删除邀请链接
     *
     * <p>删除指定的服务器邀请链接。只有具有相应权限的 Bot 才能执行此操作。
     *
     * @param urlCode 邀请链接的代码部分
     * @return 异步删除操作的结果
     * @throws IllegalArgumentException 如果邀请代码为空
     */
    @NotNull
    CompletableFuture<Void> removeInviteAsync(@NotNull String urlCode);

    // ===== 实体获取 =====

    /**
     * 异步获取用户信息
     *
     * <p>根据用户 ID 异步获取用户详细信息。
     * 如果用户在本地缓存中存在，会直接返回缓存结果。
     *
     * @param id 用户 ID
     * @return 异步结果，包含用户信息
     * @throws IllegalArgumentException 如果用户 ID 为空
     */
    @NotNull
    CompletableFuture<User> getUserAsync(@NotNull String id);

    /**
     * 异步获取服务器信息
     *
     * <p>根据服务器 ID 异步获取服务器详细信息。
     * 如果服务器在本地缓存中存在，会直接返回缓存结果。
     *
     * @param id 服务器 ID
     * @return 异步结果，包含服务器信息
     * @throws IllegalArgumentException 如果服务器 ID 为空
     */
    @NotNull
    CompletableFuture<Guild> getGuildAsync(@NotNull String id);

    /**
     * 异步获取频道信息
     *
     * <p>根据频道 ID 异步获取频道详细信息。
     * 如果频道在本地缓存中存在，会直接返回缓存结果。
     *
     * @param id 频道 ID
     * @return 异步结果，包含频道信息
     * @throws IllegalArgumentException 如果频道 ID 为空
     */
    @NotNull
    CompletableFuture<Channel> getChannelAsync(@NotNull String id);

    // ===== 批量操作 =====

    /**
     * 批量异步获取用户信息
     *
     * <p>并行获取多个用户的详细信息，显著提升获取大量用户信息的性能。
     * 所有请求会并行执行，然后汇总结果。
     *
     * <p><b>性能说明</b>：
     * 相比逐个调用 {@link #getUserAsync(String)}，批量操作可以：
     * <ul>
     *   <li>减少网络往返次数</li>
     *   <li>提高并发处理能力</li>
     *   <li>降低总体响应时间</li>
     * </ul>
     *
     * @param userIds 用户 ID 列表
     * @return 异步结果，包含用户信息列表（顺序与输入 ID 列表对应）
     * @throws IllegalArgumentException 如果 ID 列表为 null 或包含 null 元素
     */
    @NotNull
    CompletableFuture<List<User>> getBatchUsersAsync(@NotNull List<String> userIds);

    /**
     * 批量异步获取服务器信息
     *
     * <p>并行获取多个服务器的详细信息，适用于需要大量服务器信息的场景。
     *
     * @param guildIds 服务器 ID 列表
     * @return 异步结果，包含服务器信息列表（顺序与输入 ID 列表对应）
     * @throws IllegalArgumentException 如果 ID 列表为 null 或包含 null 元素
     */
    @NotNull
    CompletableFuture<List<Guild>> getBatchGuildsAsync(@NotNull List<String> guildIds);

    /**
     * 批量异步获取频道信息
     *
     * <p>并行获取多个频道的详细信息，适用于需要大量频道信息的场景。
     *
     * @param channelIds 频道 ID 列表
     * @return 异步结果，包含频道信息列表（顺序与输入 ID 列表对应）
     * @throws IllegalArgumentException 如果 ID 列表为 null 或包含 null 元素
     */
    @NotNull
    CompletableFuture<List<Channel>> getBatchChannelsAsync(@NotNull List<String> channelIds);

    // ===== 性能监控 =====

    /**
     * 获取当前正在进行的异步请求数量
     *
     * <p>用于监控系统性能和调试目的。
     * 当系统负载较高时，可以通过此方法了解当前的异步请求状况。
     *
     * @return 正在进行的异步请求数量
     */
    int getOngoingRequestCount();

    /**
     * 清理请求缓存
     *
     * <p>清理内部的请求合并缓存，主要用于测试或特殊情况。
     * 在正常使用中不需要调用此方法，系统会自动管理缓存。
     *
     * <p><b>注意</b>：此操作可能会影响正在进行的请求合并。
     */
    void clearRequestCache();
}