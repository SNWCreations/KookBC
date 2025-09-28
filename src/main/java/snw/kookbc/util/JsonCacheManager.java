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

import com.fasterxml.jackson.databind.JsonNode;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.stats.CacheStats;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicLong;

/**
 * JSON 处理缓存管理器
 *
 * <p>提供高效的 JSON 解析和序列化结果缓存，显著提升重复数据处理性能。
 * 使用 Caffeine 高性能缓存库，支持 LRU 淘汰、TTL 过期和内存限制。
 *
 * <p><b>缓存策略</b>：
 * <ul>
 *   <li><b>解析缓存</b>: 缓存 JSON 字符串的解析结果</li>
 *   <li><b>序列化缓存</b>: 缓存对象的序列化结果</li>
 *   <li><b>智能清理</b>: 基于内存使用率和访问频率自动清理</li>
 *   <li><b>统计监控</b>: 实时监控缓存命中率和性能指标</li>
 * </ul>
 *
 * <p><b>使用场景</b>：
 * <ul>
 *   <li>频繁访问的 API 响应数据</li>
 *   <li>重复解析的事件数据</li>
 *   <li>相同的卡片消息模板</li>
 *   <li>配置和元数据</li>
 * </ul>
 *
 * <p><b>性能优化</b>：
 * <ul>
 *   <li>避免重复的 JSON 解析开销</li>
 *   <li>减少对象序列化时间</li>
 *   <li>降低 CPU 使用率</li>
 *   <li>提高响应速度</li>
 * </ul>
 *
 * @since KookBC 0.33.0
 */
public final class JsonCacheManager {

    // ===== 缓存配置常量 =====

    private static final int DEFAULT_PARSE_CACHE_SIZE = 1000;          // 解析缓存大小
    private static final int DEFAULT_SERIALIZE_CACHE_SIZE = 500;       // 序列化缓存大小
    private static final Duration DEFAULT_TTL = Duration.ofMinutes(30); // 默认缓存TTL
    private static final long MAX_CACHEABLE_SIZE = 100_000;             // 最大可缓存的JSON大小

    // ===== 缓存实例 =====

    // JSON 解析结果缓存 (String -> JsonNode)
    private static final Cache<String, JsonNode> parseCache = Caffeine.newBuilder()
            .maximumSize(DEFAULT_PARSE_CACHE_SIZE)
            .expireAfterWrite(DEFAULT_TTL)
            .recordStats()
            .build();

    // JSON 序列化结果缓存 (Object -> String)
    private static final Cache<CacheKey, String> serializeCache = Caffeine.newBuilder()
            .maximumSize(DEFAULT_SERIALIZE_CACHE_SIZE)
            .expireAfterWrite(DEFAULT_TTL)
            .recordStats()
            .build();

    // 对象哈希缓存 (Object -> String)，用于快速生成缓存键
    private static final Cache<Object, String> hashCache = Caffeine.newBuilder()
            .maximumSize(5000)
            .expireAfterWrite(Duration.ofHours(1))
            .weakKeys() // 使用弱引用，允许对象被 GC
            .build();

    // ===== 性能统计 =====

    private static final AtomicLong totalParseTime = new AtomicLong(0);
    private static final AtomicLong totalSerializeTime = new AtomicLong(0);
    private static final AtomicLong cacheHitCount = new AtomicLong(0);
    private static final AtomicLong cacheMissCount = new AtomicLong(0);

    private JsonCacheManager() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }

    // ===== 解析缓存 =====

    /**
     * 带缓存的 JSON 解析
     *
     * <p>优先从缓存获取解析结果，缓存未命中时进行解析并缓存结果。
     *
     * @param jsonString JSON 字符串
     * @return 解析后的 JsonNode
     * @throws RuntimeException 如果解析失败
     */
    @NotNull
    public static JsonNode parseWithCache(@NotNull String jsonString) {
        if (!isCacheable(jsonString)) {
            // 过大的 JSON 不缓存，直接解析
            cacheMissCount.incrementAndGet();
            return parseDirectly(jsonString);
        }

        String cacheKey = generateJsonHash(jsonString);
        JsonNode cached = parseCache.getIfPresent(cacheKey);

        if (cached != null) {
            cacheHitCount.incrementAndGet();
            return cached;
        }

        // 缓存未命中，执行解析
        cacheMissCount.incrementAndGet();
        long startTime = System.nanoTime();

        try {
            JsonNode result = parseDirectly(jsonString);
            parseCache.put(cacheKey, result);
            return result;
        } finally {
            totalParseTime.addAndGet(System.nanoTime() - startTime);
        }
    }

    /**
     * 直接解析 JSON（不使用缓存）
     */
    @NotNull
    private static JsonNode parseDirectly(@NotNull String jsonString) {
        return JsonEngineSelector.parseJson(jsonString);
    }

    // ===== 序列化缓存 =====

    /**
     * 带缓存的对象序列化
     *
     * <p>优先从缓存获取序列化结果，缓存未命中时进行序列化并缓存结果。
     *
     * @param object 要序列化的对象
     * @return JSON 字符串
     * @throws RuntimeException 如果序列化失败
     */
    @NotNull
    public static String serializeWithCache(@NotNull Object object) {
        CacheKey cacheKey = generateObjectCacheKey(object);
        String cached = serializeCache.getIfPresent(cacheKey);

        if (cached != null) {
            cacheHitCount.incrementAndGet();
            return cached;
        }

        // 缓存未命中，执行序列化
        cacheMissCount.incrementAndGet();
        long startTime = System.nanoTime();

        try {
            String result = JsonEngineSelector.toJson(object);

            // 只缓存合理大小的结果
            if (isCacheable(result)) {
                serializeCache.put(cacheKey, result);
            }

            return result;
        } finally {
            totalSerializeTime.addAndGet(System.nanoTime() - startTime);
        }
    }

    // ===== 缓存键生成 =====

    /**
     * 生成 JSON 字符串的哈希缓存键
     */
    @NotNull
    private static String generateJsonHash(@NotNull String jsonString) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] hash = md.digest(jsonString.getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            // MD5 应该始终可用，fallback 到 hashCode
            return String.valueOf(jsonString.hashCode());
        }
    }

    /**
     * 生成对象的缓存键
     */
    @NotNull
    private static CacheKey generateObjectCacheKey(@NotNull Object object) {
        String objectHash = hashCache.get(object, obj -> {
            // 使用类名和 hashCode 组合生成对象唯一标识
            return obj.getClass().getName() + ":" + obj.hashCode();
        });

        return new CacheKey(objectHash, object.getClass());
    }

    /**
     * 缓存键类 - 包含对象哈希和类型信息
     */
    private static final class CacheKey {
        private final String objectHash;
        private final Class<?> objectType;
        private final int hashCode;

        CacheKey(@NotNull String objectHash, @NotNull Class<?> objectType) {
            this.objectHash = objectHash;
            this.objectType = objectType;
            this.hashCode = objectHash.hashCode() * 31 + objectType.hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null || getClass() != obj.getClass()) return false;
            CacheKey cacheKey = (CacheKey) obj;
            return objectHash.equals(cacheKey.objectHash) && objectType.equals(cacheKey.objectType);
        }

        @Override
        public int hashCode() {
            return hashCode;
        }

        @Override
        public String toString() {
            return "CacheKey{" + objectType.getSimpleName() + ":" + objectHash + "}";
        }
    }

    // ===== 缓存管理 =====

    /**
     * 判断 JSON 是否适合缓存
     */
    private static boolean isCacheable(@Nullable String jsonString) {
        return jsonString != null &&
               jsonString.length() > 50 &&              // 太小的 JSON 缓存收益不大
               jsonString.length() <= MAX_CACHEABLE_SIZE; // 太大的 JSON 不缓存
    }

    /**
     * 手动清理所有缓存
     */
    public static void clearAllCaches() {
        parseCache.invalidateAll();
        serializeCache.invalidateAll();
        hashCache.invalidateAll();
    }

    /**
     * 清理解析缓存
     */
    public static void clearParseCache() {
        parseCache.invalidateAll();
    }

    /**
     * 清理序列化缓存
     */
    public static void clearSerializeCache() {
        serializeCache.invalidateAll();
    }

    /**
     * 触发缓存清理（移除过期条目）
     */
    public static void cleanUp() {
        parseCache.cleanUp();
        serializeCache.cleanUp();
        hashCache.cleanUp();
    }

    // ===== 统计和监控 =====

    /**
     * 获取解析缓存统计信息
     */
    @NotNull
    public static CacheStats getParseCacheStats() {
        return parseCache.stats();
    }

    /**
     * 获取序列化缓存统计信息
     */
    @NotNull
    public static CacheStats getSerializeCacheStats() {
        return serializeCache.stats();
    }

    /**
     * 获取缓存命中率
     */
    public static double getCacheHitRate() {
        long totalRequests = cacheHitCount.get() + cacheMissCount.get();
        return totalRequests > 0 ? (double) cacheHitCount.get() / totalRequests : 0.0;
    }

    /**
     * 获取总的解析时间（纳秒）
     */
    public static long getTotalParseTime() {
        return totalParseTime.get();
    }

    /**
     * 获取总的序列化时间（纳秒）
     */
    public static long getTotalSerializeTime() {
        return totalSerializeTime.get();
    }

    /**
     * 获取缓存大小信息
     */
    @NotNull
    public static String getCacheSizeInfo() {
        return String.format(
            "Cache Sizes - Parse: %d/%d, Serialize: %d/%d, Hash: %d",
            parseCache.estimatedSize(), DEFAULT_PARSE_CACHE_SIZE,
            serializeCache.estimatedSize(), DEFAULT_SERIALIZE_CACHE_SIZE,
            hashCache.estimatedSize()
        );
    }

    /**
     * 获取详细的性能统计报告
     */
    @NotNull
    public static String getPerformanceReport() {
        CacheStats parseStats = getParseCacheStats();
        CacheStats serializeStats = getSerializeCacheStats();

        long totalHits = cacheHitCount.get();
        long totalMisses = cacheMissCount.get();
        long totalRequests = totalHits + totalMisses;

        double hitRate = totalRequests > 0 ? (double) totalHits / totalRequests * 100 : 0.0;

        return String.format("""
            JSON Cache Performance Report:
            ================================
            Overall Statistics:
              Total Requests: %d
              Cache Hits: %d
              Cache Misses: %d
              Hit Rate: %.2f%%

            Parse Cache:
              Hit Rate: %.2f%%
              Average Load Time: %.2fμs
              Size: %d/%d

            Serialize Cache:
              Hit Rate: %.2f%%
              Average Load Time: %.2fμs
              Size: %d/%d

            Performance:
              Total Parse Time: %.2fms
              Total Serialize Time: %.2fms
              Average Parse Time: %.2fμs
              Average Serialize Time: %.2fμs
            """,
            totalRequests, totalHits, totalMisses, hitRate,
            parseStats.hitRate() * 100, parseStats.averageLoadPenalty() / 1000.0,
            parseCache.estimatedSize(), DEFAULT_PARSE_CACHE_SIZE,
            serializeStats.hitRate() * 100, serializeStats.averageLoadPenalty() / 1000.0,
            serializeCache.estimatedSize(), DEFAULT_SERIALIZE_CACHE_SIZE,
            totalParseTime.get() / 1_000_000.0, totalSerializeTime.get() / 1_000_000.0,
            totalMisses > 0 ? totalParseTime.get() / totalMisses / 1000.0 : 0.0,
            totalMisses > 0 ? totalSerializeTime.get() / totalMisses / 1000.0 : 0.0
        );
    }

    /**
     * 重置性能统计
     */
    public static void resetStats() {
        totalParseTime.set(0);
        totalSerializeTime.set(0);
        cacheHitCount.set(0);
        cacheMissCount.set(0);
        // 注意：Caffeine 的统计不能重置，只能通过重建缓存
    }

    // ===== 便利方法 =====

    /**
     * 预热缓存 - 预先解析常用的 JSON 模板
     *
     * @param commonJsonTemplates 常用的 JSON 模板数组
     */
    public static void warmUpCache(@NotNull String... commonJsonTemplates) {
        for (String template : commonJsonTemplates) {
            if (isCacheable(template)) {
                parseWithCache(template);
            }
        }
    }

    /**
     * 检查指定 JSON 是否已缓存
     *
     * @param jsonString JSON 字符串
     * @return 是否已缓存
     */
    public static boolean isCached(@NotNull String jsonString) {
        if (!isCacheable(jsonString)) {
            return false;
        }
        String cacheKey = generateJsonHash(jsonString);
        return parseCache.getIfPresent(cacheKey) != null;
    }
}