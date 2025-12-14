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

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * 流式 JSON 处理工具
 *
 * <p>提供内存高效的大型 JSON 数据处理能力，支持流式解析、过滤、转换和生成。
 * 特别适用于处理大型 API 响应、批量数据导入/导出和实时数据流处理。
 *
 * <p><b>核心优势</b>：
 * <ul>
 *   <li><b>低内存占用</b>: 流式处理，不需要将整个 JSON 加载到内存</li>
 *   <li><b>高性能</b>: 基于 Jackson Streaming API，解析性能优异</li>
 *   <li><b>实时处理</b>: 支持边解析边处理，适合实时场景</li>
 *   <li><b>灵活过滤</b>: 支持复杂的过滤条件和转换逻辑</li>
 *   <li><b>异步支持</b>: 支持异步处理大型数据流</li>
 * </ul>
 *
 * <p><b>适用场景</b>：
 * <ul>
 *   <li>大型 API 响应数据的部分提取</li>
 *   <li>批量事件数据的实时过滤</li>
 *   <li>数据导入/导出的流式转换</li>
 *   <li>日志文件的实时分析</li>
 *   <li>配置文件的增量更新</li>
 * </ul>
 *
 * <p><b>使用示例</b>：
 * <pre>{@code
 * // 流式处理大型用户列表
 * JsonStreamProcessor.parseArray(
 *     largeJsonString,
 *     "users",
 *     user -> user.has("active") && user.get("active").asBoolean(),
 *     user -> user.get("id").asText() + ":" + user.get("name").asText()
 * ).forEach(result -> {
 *     // 处理每个活跃用户
 *     System.out.println("Active user: " + result);
 * });
 *
 * // 异步处理数据流
 * CompletableFuture<List<String>> future = JsonStreamProcessor.parseArrayAsync(
 *     inputStream,
 *     "events",
 *     event -> event.get("type").asText().equals("MESSAGE"),
 *     event -> event.get("content").asText()
 * );
 * }</pre>
 *
 * @since KookBC 0.33.0
 */
public final class JsonStreamProcessor {

    private static final ObjectMapper MAPPER = JacksonUtil.getMapper();
    private static final JsonFactory JSON_FACTORY = MAPPER.getFactory();

    // ===== 性能统计 =====
    private static final AtomicLong totalProcessedElements = new AtomicLong(0);
    private static final AtomicLong totalProcessingTime = new AtomicLong(0);
    private static final AtomicLong totalBytesProcessed = new AtomicLong(0);

    private JsonStreamProcessor() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    // ===== 核心流式处理方法 =====

    /**
     * 流式解析 JSON 数组并应用过滤和转换
     *
     * @param jsonString JSON 字符串
     * @param arrayPath 数组路径（如 "data.users"）
     * @param filter 过滤条件（可为 null）
     * @param transformer 转换函数（可为 null）
     * @param <T> 转换结果类型
     * @return 处理结果列表
     */
    @NotNull
    public static <T> List<T> parseArray(@NotNull String jsonString,
                                         @NotNull String arrayPath,
                                         @Nullable Predicate<JsonNode> filter,
                                         @Nullable Function<JsonNode, T> transformer) {
        List<T> results = new ArrayList<>();
        long startTime = System.nanoTime();

        try (JsonParser parser = JSON_FACTORY.createParser(jsonString)) {
            processArrayStream(parser, arrayPath, filter, transformer, results::add);
            return results;
        } catch (IOException e) {
            throw new RuntimeException("Failed to parse JSON array stream", e);
        } finally {
            totalProcessingTime.addAndGet(System.nanoTime() - startTime);
            totalBytesProcessed.addAndGet(jsonString.length());
        }
    }

    /**
     * 异步流式解析 JSON 数组
     *
     * @param inputStream 输入流
     * @param arrayPath 数组路径
     * @param filter 过滤条件
     * @param transformer 转换函数
     * @param <T> 转换结果类型
     * @return 异步处理结果
     */
    @NotNull
    public static <T> CompletableFuture<List<T>> parseArrayAsync(@NotNull InputStream inputStream,
                                                                 @NotNull String arrayPath,
                                                                 @Nullable Predicate<JsonNode> filter,
                                                                 @Nullable Function<JsonNode, T> transformer) {
        return CompletableFuture.supplyAsync(() -> {
            List<T> results = new ArrayList<>();
            long startTime = System.nanoTime();
            long bytesRead = 0;

            try (JsonParser parser = JSON_FACTORY.createParser(inputStream)) {
                processArrayStream(parser, arrayPath, filter, transformer, results::add);
                return results;
            } catch (IOException e) {
                throw new RuntimeException("Failed to parse JSON array stream asynchronously", e);
            } finally {
                totalProcessingTime.addAndGet(System.nanoTime() - startTime);
                totalBytesProcessed.addAndGet(bytesRead);
            }
        }, VirtualThreadUtil.getJsonExecutor());
    }

    /**
     * 流式处理 JSON 数组，实时回调处理每个元素
     *
     * @param jsonString JSON 字符串
     * @param arrayPath 数组路径
     * @param filter 过滤条件
     * @param processor 元素处理器
     */
    public static void processArrayStream(@NotNull String jsonString,
                                          @NotNull String arrayPath,
                                          @Nullable Predicate<JsonNode> filter,
                                          @NotNull Consumer<JsonNode> processor) {
        long startTime = System.nanoTime();

        try (JsonParser parser = JSON_FACTORY.createParser(jsonString)) {
            processArrayStream(parser, arrayPath, filter, null, processor);
        } catch (IOException e) {
            throw new RuntimeException("Failed to process JSON array stream", e);
        } finally {
            totalProcessingTime.addAndGet(System.nanoTime() - startTime);
            totalBytesProcessed.addAndGet(jsonString.length());
        }
    }

    /**
     * 核心流式数组处理逻辑
     */
    private static <T> void processArrayStream(@NotNull JsonParser parser,
                                               @NotNull String arrayPath,
                                               @Nullable Predicate<JsonNode> filter,
                                               @Nullable Function<JsonNode, T> transformer,
                                               @NotNull Consumer<T> consumer) throws IOException {
        // 导航到指定的数组路径
        if (!navigateToPath(parser, arrayPath)) {
            return; // 路径不存在
        }

        // 确保当前位置是数组开始
        if (parser.getCurrentToken() != JsonToken.START_ARRAY) {
            throw new IllegalArgumentException("Path '" + arrayPath + "' does not point to an array");
        }

        // 逐个处理数组元素
        while (parser.nextToken() != JsonToken.END_ARRAY) {
            if (parser.getCurrentToken() == JsonToken.START_OBJECT ||
                parser.getCurrentToken() == JsonToken.START_ARRAY) {

                // 解析当前元素为 JsonNode
                JsonNode element = MAPPER.readTree(parser);
                totalProcessedElements.incrementAndGet();

                // 应用过滤器
                if (filter == null || filter.test(element)) {
                    T result;
                    if (transformer != null) {
                        result = transformer.apply(element);
                    } else {
                        @SuppressWarnings("unchecked")
                        T elementAsT = (T) element;
                        result = elementAsT;
                    }
                    consumer.accept(result);
                }
            }
        }
    }

    /**
     * 导航到指定的 JSON 路径
     *
     * @param parser JSON 解析器
     * @param path 路径字符串（如 "data.users"）
     * @return 是否成功找到路径
     */
    private static boolean navigateToPath(@NotNull JsonParser parser, @NotNull String path) throws IOException {
        String[] pathSegments = path.split("\\.");

        // 寻找根对象
        if (parser.nextToken() != JsonToken.START_OBJECT) {
            return false;
        }

        // 逐级导航
        for (String segment : pathSegments) {
            if (!navigateToField(parser, segment)) {
                return false;
            }
        }

        return true;
    }

    /**
     * 导航到指定字段
     */
    private static boolean navigateToField(@NotNull JsonParser parser, @NotNull String fieldName) throws IOException {
        while (parser.nextToken() != JsonToken.END_OBJECT) {
            if (parser.getCurrentToken() == JsonToken.FIELD_NAME) {
                if (fieldName.equals(parser.getCurrentName())) {
                    parser.nextToken(); // 移动到字段值
                    return true;
                } else {
                    parser.nextToken(); // 跳过不匹配的字段值
                    parser.skipChildren();
                }
            }
        }
        return false;
    }

    // ===== 流式生成器 =====

    /**
     * 流式 JSON 生成器
     *
     * <p>提供内存高效的大型 JSON 数据生成能力。
     */
    public static class JsonStreamGenerator implements AutoCloseable {
        private final JsonGenerator generator;
        private final StringWriter stringWriter;
        private boolean closed = false;

        /**
         * 创建字符串输出的流式生成器
         */
        @NotNull
        public static JsonStreamGenerator createStringGenerator() {
            try {
                StringWriter stringWriter = new StringWriter();
                JsonGenerator generator = JSON_FACTORY.createGenerator(stringWriter);
                generator.useDefaultPrettyPrinter(); // 格式化输出
                return new JsonStreamGenerator(generator, stringWriter);
            } catch (IOException e) {
                throw new RuntimeException("Failed to create JSON stream generator", e);
            }
        }

        /**
         * 创建文件输出的流式生成器
         */
        @NotNull
        public static JsonStreamGenerator createFileGenerator(@NotNull File outputFile) {
            try {
                JsonGenerator generator = JSON_FACTORY.createGenerator(outputFile, com.fasterxml.jackson.core.JsonEncoding.UTF8);
                generator.useDefaultPrettyPrinter();
                return new JsonStreamGenerator(generator, null);
            } catch (IOException e) {
                throw new RuntimeException("Failed to create JSON file stream generator", e);
            }
        }

        private JsonStreamGenerator(@NotNull JsonGenerator generator, @Nullable StringWriter stringWriter) {
            this.generator = generator;
            this.stringWriter = stringWriter;
        }

        /**
         * 开始生成对象
         */
        @NotNull
        public JsonStreamGenerator startObject() {
            try {
                generator.writeStartObject();
                return this;
            } catch (IOException e) {
                throw new RuntimeException("Failed to write start object", e);
            }
        }

        /**
         * 结束对象生成
         */
        @NotNull
        public JsonStreamGenerator endObject() {
            try {
                generator.writeEndObject();
                return this;
            } catch (IOException e) {
                throw new RuntimeException("Failed to write end object", e);
            }
        }

        /**
         * 开始生成数组
         */
        @NotNull
        public JsonStreamGenerator startArray(@NotNull String fieldName) {
            try {
                generator.writeArrayFieldStart(fieldName);
                return this;
            } catch (IOException e) {
                throw new RuntimeException("Failed to write array field start", e);
            }
        }

        /**
         * 结束数组生成
         */
        @NotNull
        public JsonStreamGenerator endArray() {
            try {
                generator.writeEndArray();
                return this;
            } catch (IOException e) {
                throw new RuntimeException("Failed to write end array", e);
            }
        }

        /**
         * 写入字段值
         */
        @NotNull
        public JsonStreamGenerator writeField(@NotNull String fieldName, @Nullable Object value) {
            try {
                if (value == null) {
                    generator.writeNullField(fieldName);
                } else if (value instanceof String) {
                    generator.writeStringField(fieldName, (String) value);
                } else if (value instanceof Integer) {
                    generator.writeNumberField(fieldName, (Integer) value);
                } else if (value instanceof Long) {
                    generator.writeNumberField(fieldName, (Long) value);
                } else if (value instanceof Double) {
                    generator.writeNumberField(fieldName, (Double) value);
                } else if (value instanceof Boolean) {
                    generator.writeBooleanField(fieldName, (Boolean) value);
                } else {
                    // 复杂对象使用 ObjectMapper 序列化
                    generator.writeFieldName(fieldName);
                    MAPPER.writeValue(generator, value);
                }
                return this;
            } catch (IOException e) {
                throw new RuntimeException("Failed to write field: " + fieldName, e);
            }
        }

        /**
         * 写入数组元素
         */
        @NotNull
        public JsonStreamGenerator writeArrayElement(@Nullable Object value) {
            try {
                if (value == null) {
                    generator.writeNull();
                } else {
                    MAPPER.writeValue(generator, value);
                }
                return this;
            } catch (IOException e) {
                throw new RuntimeException("Failed to write array element", e);
            }
        }

        /**
         * 刷新输出缓冲区
         */
        @NotNull
        public JsonStreamGenerator flush() {
            try {
                generator.flush();
                return this;
            } catch (IOException e) {
                throw new RuntimeException("Failed to flush generator", e);
            }
        }

        /**
         * 获取生成的 JSON 字符串（仅适用于字符串生成器）
         */
        @NotNull
        public String toString() {
            if (stringWriter == null) {
                throw new UnsupportedOperationException("toString() is only available for string generators");
            }
            try {
                generator.flush();
                return stringWriter.toString();
            } catch (IOException e) {
                throw new RuntimeException("Failed to get generated JSON string", e);
            }
        }

        @Override
        public void close() {
            if (!closed) {
                try {
                    generator.close();
                    closed = true;
                } catch (IOException e) {
                    throw new RuntimeException("Failed to close JSON generator", e);
                }
            }
        }
    }

    // ===== 便利方法 =====

    /**
     * 快速提取数组中的特定字段
     *
     * @param jsonString JSON 字符串
     * @param arrayPath 数组路径
     * @param fieldName 要提取的字段名
     * @return 字段值列表
     */
    @NotNull
    public static List<String> extractFieldValues(@NotNull String jsonString,
                                                  @NotNull String arrayPath,
                                                  @NotNull String fieldName) {
        return parseArray(
            jsonString,
            arrayPath,
            node -> node.has(fieldName) && !node.get(fieldName).isNull(),
            node -> node.get(fieldName).asText()
        );
    }

    /**
     * 统计数组元素数量（不加载全部数据到内存）
     *
     * @param jsonString JSON 字符串
     * @param arrayPath 数组路径
     * @param filter 过滤条件（可为 null）
     * @return 元素数量
     */
    public static long countArrayElements(@NotNull String jsonString,
                                          @NotNull String arrayPath,
                                          @Nullable Predicate<JsonNode> filter) {
        AtomicLong count = new AtomicLong(0);
        processArrayStream(jsonString, arrayPath, filter, node -> count.incrementAndGet());
        return count.get();
    }

    // ===== 性能监控 =====

    /**
     * 获取已处理的元素总数
     */
    public static long getTotalProcessedElements() {
        return totalProcessedElements.get();
    }

    /**
     * 获取总处理时间（纳秒）
     */
    public static long getTotalProcessingTime() {
        return totalProcessingTime.get();
    }

    /**
     * 获取总处理字节数
     */
    public static long getTotalBytesProcessed() {
        return totalBytesProcessed.get();
    }

    /**
     * 获取平均处理速度（元素/秒）
     */
    public static double getAverageProcessingSpeed() {
        long elements = totalProcessedElements.get();
        long timeNanos = totalProcessingTime.get();
        return timeNanos > 0 ? (elements * 1_000_000_000.0) / timeNanos : 0.0;
    }

    /**
     * 获取性能统计报告
     */
    @NotNull
    public static String getPerformanceReport() {
        long elements = getTotalProcessedElements();
        long timeNanos = getTotalProcessingTime();
        long bytes = getTotalBytesProcessed();
        double speed = getAverageProcessingSpeed();

        return String.format("""
            JSON Stream Processing Performance:
            ===================================
            Total Elements Processed: %d
            Total Processing Time: %.2fms
            Total Bytes Processed: %d (%.2fMB)
            Average Processing Speed: %.2f elements/sec
            Average Throughput: %.2fMB/sec
            """,
            elements,
            timeNanos / 1_000_000.0,
            bytes, bytes / 1_048_576.0,
            speed,
            timeNanos > 0 ? (bytes * 1_000_000_000.0 / 1_048_576.0) / timeNanos : 0.0
        );
    }

    /**
     * 重置性能统计
     */
    public static void resetStats() {
        totalProcessedElements.set(0);
        totalProcessingTime.set(0);
        totalBytesProcessed.set(0);
    }
}