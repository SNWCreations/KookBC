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

package snw.kookbc.impl.network.ws;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.UnknownHostException;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 智能重连策略
 *
 * <p>提供完整的断线重连机制，包括：
 * <ul>
 *   <li>指数退避算法 - 避免频繁重连</li>
 *   <li>无限重试模式 - 持续重连直到成功或遇到不可恢复异常</li>
 *   <li>异常分类处理 - 区分可恢复和不可恢复错误</li>
 *   <li>统计监控 - 记录重连历史和性能指标</li>
 * </ul>
 *
 * <p><b>指数退避序列</b>（秒）：
 * <pre>1, 2, 4, 8, 16, 32, 60, 60, 60...</pre>
 *
 * <p><b>异常处理策略</b>：
 * <ul>
 *   <li>网络异常（DNS、连接超时、IO错误）- 允许无限重连</li>
 *   <li>认证失败（401/403）- 不允许重连，需要用户介入</li>
 *   <li>其他异常 - 记录后允许重连</li>
 * </ul>
 *
 * <p><b>自动重置</b>：
 * <ul>
 *   <li>连接保持稳定 5 分钟后自动重置重试计数器</li>
 * </ul>
 *
 * @since KookBC 0.53.0
 */
public class ReconnectStrategy {
    private static final Logger logger = LoggerFactory.getLogger(ReconnectStrategy.class);

    // ===== 重连配置常量 =====

    /**
     * 指数退避延迟序列（秒）
     * 1, 2, 4, 8, 16, 32, 60, 60...
     */
    private static final int[] BACKOFF_DELAYS = {1, 2, 4, 8, 16, 32, 60};

    /**
     * 最大退避延迟（秒）
     */
    private static final int MAX_BACKOFF_DELAY = 60;

    /**
     * 成功连接后重置计数器的等待时间（秒）
     * 如果连接保持稳定5分钟，则认为连接恢复正常，重置重试计数
     */
    private static final int RESET_THRESHOLD_SECONDS = 300;

    // ===== 重连状态 =====

    private final AtomicInteger attemptCount = new AtomicInteger(0);
    private final AtomicLong totalReconnects = new AtomicLong(0);
    private final AtomicLong successfulReconnects = new AtomicLong(0);
    private final AtomicLong failedReconnects = new AtomicLong(0);

    private volatile Instant lastSuccessfulConnection = null;
    private volatile Instant lastAttemptTime = null;
    private volatile Throwable lastException = null;

    /**
     * 创建重连策略（无限重试）
     */
    public ReconnectStrategy() {
        // 无需初始化，重连将持续到连接成功或遇到不可恢复的异常
    }

    /**
     * 检查是否应该重连
     *
     * @param exception 导致断线的异常（可为 null）
     * @return true 如果应该重连，false 如果应该放弃
     */
    public boolean shouldReconnect(Throwable exception) {
        lastException = exception;
        lastAttemptTime = Instant.now();

        // 分析异常类型
        if (exception != null) {
            if (isUnrecoverableException(exception)) {
                logger.error("遇到不可恢复的异常，停止重连: {}", exception.getMessage());
                return false;
            }

            // 记录可恢复的异常类型
            logRecoverableException(exception);
        }

        // 无限重试，只要不是不可恢复的异常就继续重连
        return true;
    }

    /**
     * 判断异常是否不可恢复
     *
     * @param exception 异常对象
     * @return true 如果是不可恢复的异常
     */
    private boolean isUnrecoverableException(Throwable exception) {
        String message = exception.getMessage();
        if (message == null) {
            return false;
        }

        // 认证失败 - 需要用户更新 Token
        if (message.contains("401") || message.contains("Unauthorized") || message.contains("Invalid token")) {
            logger.error("认证失败，请检查 Bot Token 是否正确");
            return true;
        }

        // 403 - Bot 被封禁
        if (message.contains("403") || message.contains("Forbidden")) {
            logger.error("Bot 访问被拒绝，可能被封禁");
            return true;
        }

        return false;
    }

    /**
     * 记录可恢复的异常信息
     */
    private void logRecoverableException(Throwable exception) {
        String exceptionType = exception.getClass().getSimpleName();
        String message = exception.getMessage();

        // DNS 解析失败
        if (exception instanceof UnknownHostException) {
            logger.warn("DNS 解析失败: {}，将在延迟后重试（可能是网络或 DNS 问题）", message);
            return;
        }

        // 连接超时
        if (exceptionType.contains("Timeout") || (message != null && message.contains("timeout"))) {
            logger.warn("连接超时: {}，将在延迟后重试", message);
            return;
        }

        // IO 异常
        if (exceptionType.contains("IOException")) {
            logger.warn("网络 I/O 异常: {}，将在延迟后重试", message);
            return;
        }

        // 其他可恢复异常
        logger.warn("遇到可恢复异常 ({}): {}，将在延迟后重试", exceptionType, message);
    }

    /**
     * 计算下一次重连的延迟时间（秒）
     *
     * @return 延迟秒数
     */
    public int getNextDelay() {
        int attempt = attemptCount.getAndIncrement();
        totalReconnects.incrementAndGet();

        // 使用指数退避算法
        int delay;
        if (attempt < BACKOFF_DELAYS.length) {
            delay = BACKOFF_DELAYS[attempt];
        } else {
            delay = MAX_BACKOFF_DELAY;
        }

        logger.info("重连延迟: {} 秒 (第 {} 次重试，将持续重试直到连接成功)", delay, attempt + 1);
        return delay;
    }

    /**
     * 执行延迟等待
     *
     * @param delaySeconds 延迟秒数
     * @return true 如果成功等待，false 如果被中断
     */
    public boolean waitBeforeReconnect(int delaySeconds) {
        try {
            logger.debug("等待 {} 秒后重连...", delaySeconds);
            TimeUnit.SECONDS.sleep(delaySeconds);
            return true;
        } catch (InterruptedException e) {
            logger.warn("重连等待被中断");
            Thread.currentThread().interrupt();
            return false;
        }
    }

    /**
     * 标记连接成功
     * <p>检查是否应该重置重试计数器
     */
    public void onConnectionSuccess() {
        Instant now = Instant.now();

        // 如果上次成功连接已经是 5 分钟前，重置计数器
        if (lastSuccessfulConnection != null) {
            Duration stableTime = Duration.between(lastSuccessfulConnection, now);
            if (stableTime.getSeconds() >= RESET_THRESHOLD_SECONDS) {
                logger.info("连接保持稳定 {} 分钟，重置重连计数器", stableTime.toMinutes());
                reset();
            }
        }

        lastSuccessfulConnection = now;
        if (attemptCount.get() > 0) {
            successfulReconnects.incrementAndGet();
            logger.info("重连成功！（共尝试 {} 次）", attemptCount.get());
        }
    }

    /**
     * 标记连接失败
     */
    public void onConnectionFailure() {
        failedReconnects.incrementAndGet();
    }

    /**
     * 重置重连状态
     */
    public void reset() {
        attemptCount.set(0);
        lastException = null;
    }

    /**
     * 完全重置所有统计信息
     */
    public void fullReset() {
        reset();
        totalReconnects.set(0);
        successfulReconnects.set(0);
        failedReconnects.set(0);
        lastSuccessfulConnection = null;
        lastAttemptTime = null;
    }

    // ===== 统计信息 =====

    /**
     * 获取当前重试次数
     */
    public int getCurrentAttempt() {
        return attemptCount.get();
    }

    /**
     * 获取总重连次数
     */
    public long getTotalReconnects() {
        return totalReconnects.get();
    }

    /**
     * 获取成功重连次数
     */
    public long getSuccessfulReconnects() {
        return successfulReconnects.get();
    }

    /**
     * 获取失败重连次数
     */
    public long getFailedReconnects() {
        return failedReconnects.get();
    }

    /**
     * 获取最后一次异常
     */
    public Throwable getLastException() {
        return lastException;
    }

    /**
     * 获取上次成功连接时间
     */
    public Instant getLastSuccessfulConnection() {
        return lastSuccessfulConnection;
    }

    /**
     * 获取上次尝试重连时间
     */
    public Instant getLastAttemptTime() {
        return lastAttemptTime;
    }

    /**
     * 获取重连成功率
     */
    public double getSuccessRate() {
        long total = totalReconnects.get();
        return total > 0 ? (double) successfulReconnects.get() / total : 0.0;
    }

    /**
     * 获取统计报告
     */
    public String getStatisticsReport() {
        return String.format(
            """
            重连统计报告:
            ===========================================
            当前重试: %d (无��重试模式)
            总重连次数: %d
            成功重连: %d
            失败重连: %d
            成功率: %.2f%%
            上次成功连接: %s
            上次尝试时间: %s
            上次异常: %s
            """,
            getCurrentAttempt(),
            getTotalReconnects(),
            getSuccessfulReconnects(),
            getFailedReconnects(),
            getSuccessRate() * 100,
            lastSuccessfulConnection != null ? lastSuccessfulConnection.toString() : "N/A",
            lastAttemptTime != null ? lastAttemptTime.toString() : "N/A",
            lastException != null ? lastException.getMessage() : "N/A"
        );
    }

    @Override
    public String toString() {
        return String.format("ReconnectStrategy[attempt=%d (unlimited), success=%.2f%%]",
            getCurrentAttempt(), getSuccessRate() * 100);
    }
}
