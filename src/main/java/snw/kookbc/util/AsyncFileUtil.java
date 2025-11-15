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

package snw.kookbc.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

/**
 * 异步文件操作工具类 - 基于虚拟线程的高性能文件 I/O
 *
 * <p>使用虚拟线程处理文件 I/O 操作，避免阻塞主线程，
 * 特别适合处理大量文件操作或大文件操作的场景。
 *
 * @since Java 21
 */
public final class AsyncFileUtil {

    private AsyncFileUtil() {
        // 工具类，禁止实例化
    }

    // ===== 异步文件读取 =====

    /**
     * 异步读取文件全部内容为字符串
     *
     * @param path 文件路径
     * @return 异步文件内容
     */
    public static CompletableFuture<String> readStringAsync(Path path) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return Files.readString(path);
            } catch (IOException e) {
                throw new RuntimeException("读取文件失败: " + path, e);
            }
        }, VirtualThreadUtil.getFileIoExecutor());
    }

    /**
     * 异步读取文件所有行
     *
     * @param path 文件路径
     * @return 异步文件行列表
     */
    public static CompletableFuture<List<String>> readAllLinesAsync(Path path) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return Files.readAllLines(path);
            } catch (IOException e) {
                throw new RuntimeException("读取文件行失败: " + path, e);
            }
        }, VirtualThreadUtil.getFileIoExecutor());
    }

    /**
     * 异步读取文件全部字节
     *
     * @param path 文件路径
     * @return 异步文件字节数组
     */
    public static CompletableFuture<byte[]> readAllBytesAsync(Path path) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return Files.readAllBytes(path);
            } catch (IOException e) {
                throw new RuntimeException("读取文件字节失败: " + path, e);
            }
        }, VirtualThreadUtil.getFileIoExecutor());
    }

    // ===== 异步文件写入 =====

    /**
     * 异步写入字符串到文件
     *
     * @param path 文件路径
     * @param content 文件内容
     * @return 异步写入结果
     */
    public static CompletableFuture<Void> writeStringAsync(Path path, String content) {
        return CompletableFuture.runAsync(() -> {
            try {
                Files.writeString(path, content);
            } catch (IOException e) {
                throw new RuntimeException("写入文件失败: " + path, e);
            }
        }, VirtualThreadUtil.getFileIoExecutor());
    }

    /**
     * 异步追加字符串到文件
     *
     * @param path 文件路径
     * @param content 要追加的内容
     * @return 异步写入结果
     */
    public static CompletableFuture<Void> appendStringAsync(Path path, String content) {
        return CompletableFuture.runAsync(() -> {
            try {
                Files.writeString(path, content, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            } catch (IOException e) {
                throw new RuntimeException("追加文件失败: " + path, e);
            }
        }, VirtualThreadUtil.getFileIoExecutor());
    }

    /**
     * 异步写入行列表到文件
     *
     * @param path 文件路径
     * @param lines 行列表
     * @return 异步写入结果
     */
    public static CompletableFuture<Void> writeAllLinesAsync(Path path, List<String> lines) {
        return CompletableFuture.runAsync(() -> {
            try {
                Files.write(path, lines);
            } catch (IOException e) {
                throw new RuntimeException("写入文件行失败: " + path, e);
            }
        }, VirtualThreadUtil.getFileIoExecutor());
    }

    /**
     * 异步写入字节数组到文件
     *
     * @param path 文件路径
     * @param bytes 字节数组
     * @return 异步写入结果
     */
    public static CompletableFuture<Void> writeAllBytesAsync(Path path, byte[] bytes) {
        return CompletableFuture.runAsync(() -> {
            try {
                Files.write(path, bytes);
            } catch (IOException e) {
                throw new RuntimeException("写入文件字节失败: " + path, e);
            }
        }, VirtualThreadUtil.getFileIoExecutor());
    }

    // ===== 异步文件操作 =====

    /**
     * 异步检查文件是否存在
     *
     * @param path 文件路径
     * @return 异步结果
     */
    public static CompletableFuture<Boolean> existsAsync(Path path) {
        return CompletableFuture.supplyAsync(() -> Files.exists(path), VirtualThreadUtil.getFileIoExecutor());
    }

    /**
     * 异步创建目录
     *
     * @param path 目录路径
     * @return 异步创建结果
     */
    public static CompletableFuture<Path> createDirectoriesAsync(Path path) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return Files.createDirectories(path);
            } catch (IOException e) {
                throw new RuntimeException("创建目录失败: " + path, e);
            }
        }, VirtualThreadUtil.getFileIoExecutor());
    }

    /**
     * 异步删除文件
     *
     * @param path 文件路径
     * @return 异步删除结果
     */
    public static CompletableFuture<Void> deleteAsync(Path path) {
        return CompletableFuture.runAsync(() -> {
            try {
                Files.deleteIfExists(path);
            } catch (IOException e) {
                throw new RuntimeException("删除文件失败: " + path, e);
            }
        }, VirtualThreadUtil.getFileIoExecutor());
    }

    /**
     * 异步复制文件
     *
     * @param source 源文件路径
     * @param target 目标文件路径
     * @return 异步复制结果
     */
    public static CompletableFuture<Path> copyAsync(Path source, Path target) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return Files.copy(source, target);
            } catch (IOException e) {
                throw new RuntimeException("复制文件失败: " + source + " -> " + target, e);
            }
        }, VirtualThreadUtil.getFileIoExecutor());
    }

    /**
     * 异步移动文件
     *
     * @param source 源文件路径
     * @param target 目标文件路径
     * @return 异步移动结果
     */
    public static CompletableFuture<Path> moveAsync(Path source, Path target) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return Files.move(source, target);
            } catch (IOException e) {
                throw new RuntimeException("移动文件失败: " + source + " -> " + target, e);
            }
        }, VirtualThreadUtil.getFileIoExecutor());
    }

    // ===== 批量文件操作 =====

    /**
     * 批量异步读取多个文件
     *
     * @param paths 文件路径列表
     * @return 异步结果列表
     */
    public static CompletableFuture<List<String>> batchReadStringAsync(List<Path> paths) {
        List<CompletableFuture<String>> futures = paths.stream()
            .map(AsyncFileUtil::readStringAsync)
            .collect(Collectors.toList());

        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
            .thenApply(v -> futures.stream()
                .map(CompletableFuture::join)
                .collect(Collectors.toList()));
    }

    /**
     * 批量异步写入多个文件
     *
     * @param pathContentMap 路径和内容的映射
     * @return 异步写入结果
     */
    public static CompletableFuture<Void> batchWriteStringAsync(java.util.Map<Path, String> pathContentMap) {
        List<CompletableFuture<Void>> futures = pathContentMap.entrySet().stream()
            .map(entry -> writeStringAsync(entry.getKey(), entry.getValue()))
            .collect(Collectors.toList());

        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
    }

    // ===== 高级功能 =====

    /**
     * 异步备份文件（复制到 .backup 后缀）
     *
     * @param path 原文件路径
     * @return 异步备份结果
     */
    public static CompletableFuture<Path> backupAsync(Path path) {
        Path backupPath = Path.of(path.toString() + ".backup");
        return copyAsync(path, backupPath);
    }

    /**
     * 异步安全写入文件（先写临时文件，再原子性替换）
     *
     * @param path 目标文件路径
     * @param content 文件内容
     * @return 异步写入结果
     */
    public static CompletableFuture<Void> safeWriteStringAsync(Path path, String content) {
        return CompletableFuture.runAsync(() -> {
            Path tempPath = Path.of(path.toString() + ".tmp");
            try {
                // 写入临时文件
                Files.writeString(tempPath, content);
                // 原子性替换
                Files.move(tempPath, path);
            } catch (IOException e) {
                // 清理临时文件
                try {
                    Files.deleteIfExists(tempPath);
                } catch (IOException cleanupEx) {
                    e.addSuppressed(cleanupEx);
                }
                throw new RuntimeException("安全写入文件失败: " + path, e);
            }
        }, VirtualThreadUtil.getFileIoExecutor());
    }
}