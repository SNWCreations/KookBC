/*
 *     KookBC -- The Kook Bot Client & JKook API standard implementation for Java.
 *     Copyright (C) 2022 - 2023 SNWCreations and contributors
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU Affero General Public License as
 *     published by the Free Software Foundation, either version 3 of the
 *     License, or (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU Affero General Public License for more details.
 *
 *     You should have received a copy of the GNU Affero General Public License
 *     along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package snw.kookbc.test;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.helpers.NOPLogger;
import snw.jkook.JKook;
import snw.jkook.config.file.YamlConfiguration;
import snw.kookbc.impl.CoreImpl;
import snw.kookbc.impl.KBCClient;

import java.io.File;

/**
 * 测试基类，提供通用的测试设置和清理功能
 */
@ExtendWith(MockitoExtension.class)
public abstract class BaseTest {

    private static boolean coreInitialized = false;
    private static final Object coreLock = new Object();

    /**
     * 在每个测试方法执行前运行的设置代码
     */
    @BeforeEach
    protected void setUp() {
        // 设置测试环境
        System.setProperty("kbc.test.mode", "true");
        System.setProperty("java.util.logging.manager", "org.apache.logging.log4j.jul.LogManager");

        // 调用子类的设置方法
        doSetUp();
    }

    /**
     * 在每个测试方法执行后运行的清理代码
     */
    @AfterEach
    protected void tearDown() {
        // 调用子类的清理方法
        doTearDown();

        // 清理测试环境
        System.clearProperty("kbc.test.mode");
    }

    /**
     * 子类可以重写此方法来提供额外的设置逻辑
     */
    protected void doSetUp() {
        // 默认为空，子类可重写
    }

    /**
     * 子类可以重写此方法来提供额外的清理逻辑
     */
    protected void doTearDown() {
        // 默认为空，子类可重写
    }

    /**
     * 检查当前是否处于测试模式
     *
     * @return 如果处于测试模式返回 true，否则返回 false
     */
    protected boolean isTestMode() {
        return Boolean.parseBoolean(System.getProperty("kbc.test.mode", "false"));
    }

    /**
     * 等待指定的毫秒数，用于测试中的时序控制
     *
     * @param millis 等待的毫秒数
     */
    protected void waitFor(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("测试被中断", e);
        }
    }

    /**
     * 创建测试用的KBCClient实例
     * 使用测试token和基本配置
     *
     * @return 配置好的KBCClient实例
     */
    protected KBCClient createTestKBCClient() {
        return createTestKBCClient("Bot test-token-for-unit-test");
    }

    /**
     * 创建测试用的KBCClient实例
     *
     * @param token 测试用的Bot token
     * @return 配置好的KBCClient实例
     */
    protected KBCClient createTestKBCClient(String token) {
        CoreImpl core = new CoreImpl(NOPLogger.NOP_LOGGER);
        YamlConfiguration config = createTestConfiguration();

        // 使用null插件目录以避免目录验证问题
        File pluginsFolder = null;

        // 线程安全地确保只设置一次Core
        synchronized (coreLock) {
            if (!coreInitialized) {
                JKook.setCore(core);
                coreInitialized = true;
            }
        }

        return new KBCClient(core, config, pluginsFolder, token);
    }

    /**
     * 创建测试用的配置对象
     *
     * @return 测试用的YAML配置
     */
    protected YamlConfiguration createTestConfiguration() {
        YamlConfiguration config = new YamlConfiguration();
        config.set("mode", "websocket");
        config.set("compress", true);
        config.set("check-update", false);
        config.set("ignore-ssl", false);
        config.set("webhook-port", 8080);
        config.set("webhook-route", "kookbc-webhook");
        config.set("webhook-encrypt-key", "");
        config.set("webhook-verify-token", "");
        config.set("internal-commands-reply-result-type", "REPLY");
        config.set("ignore-remote-call-invisible-internal-command", true);
        config.set("over-limit-warning-log-level", "DEBUG");
        config.set("allow-warn-old-message", true);
        config.set("allow-error-feedback", true);
        return config;
    }
}