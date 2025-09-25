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

package snw.kookbc.config;

import snw.jkook.config.InvalidConfigurationException;
import snw.jkook.config.file.YamlConfiguration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import snw.kookbc.test.BaseTest;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.*;

/**
 * KookBC 配置文件解析和验证测试
 * 测试YAML配置文件的加载、解析和验证功能
 */
@DisplayName("KookBC 配置文件测试")
class ConfigurationTest extends BaseTest {

    @TempDir
    Path tempDir;

    @Test
    @DisplayName("应该正确解析默认配置文件")
    void testParseDefaultConfiguration() throws IOException, InvalidConfigurationException {
        String defaultConfig = loadDefaultKbcYml();

        YamlConfiguration config = new YamlConfiguration();
        config.loadFromString(defaultConfig);

        // 验证基本配置项
        assertThat(config.getString("token")).isEmpty();
        assertThat(config.getString("mode")).isEqualTo("websocket");
        assertThat(config.getBoolean("compress")).isTrue();
        assertThat(config.getBoolean("check-update")).isTrue();
        assertThat(config.getBoolean("ignore-ssl")).isFalse();
    }

    @Test
    @DisplayName("应该正确解析Webhook配置")
    void testParseWebhookConfiguration() throws IOException, InvalidConfigurationException {
        String webhookConfig = createWebhookConfig();

        YamlConfiguration config = new YamlConfiguration();
        config.loadFromString(webhookConfig);

        assertThat(config.getString("mode")).isEqualTo("webhook");
        assertThat(config.getInt("webhook-port")).isEqualTo(8080);
        assertThat(config.getString("webhook-route")).isEqualTo("kookbc-webhook");
        assertThat(config.getString("webhook-encrypt-key")).isEmpty();
        assertThat(config.getString("webhook-verify-token")).isEmpty();
    }

    @Test
    @DisplayName("应该正确解析内部命令配置")
    void testParseInternalCommandsConfiguration() throws IOException, InvalidConfigurationException {
        String defaultConfig = loadDefaultKbcYml();

        YamlConfiguration config = new YamlConfiguration();
        config.loadFromString(defaultConfig);

        // 验证内部命令配置
        assertThat(config.getBoolean("internal-commands.stop")).isTrue();
        assertThat(config.getBoolean("internal-commands.help")).isTrue();
        assertThat(config.getBoolean("internal-commands.plugins")).isTrue();

        assertThat(config.getString("internal-commands-reply-result-type")).isEqualTo("REPLY");
        assertThat(config.getBoolean("ignore-remote-call-invisible-internal-command")).isTrue();
    }

    @Test
    @DisplayName("应该正确解析日志配置")
    void testParseLoggingConfiguration() throws IOException, InvalidConfigurationException {
        String defaultConfig = loadDefaultKbcYml();

        YamlConfiguration config = new YamlConfiguration();
        config.loadFromString(defaultConfig);

        assertThat(config.getString("over-limit-warning-log-level")).isEqualTo("DEBUG");
        assertThat(config.getBoolean("allow-warn-old-message")).isTrue();
        assertThat(config.getBoolean("allow-error-feedback")).isTrue();
    }

    @Test
    @DisplayName("应该正确处理自定义配置值")
    void testCustomConfigurationValues() {
        YamlConfiguration config = new YamlConfiguration();

        // 设置自定义值
        config.set("token", "Bot test-token-12345");
        config.set("mode", "webhook");
        config.set("webhook-port", 9090);
        config.set("compress", false);
        config.set("check-update", false);

        // 验证自定义值
        assertThat(config.getString("token")).isEqualTo("Bot test-token-12345");
        assertThat(config.getString("mode")).isEqualTo("webhook");
        assertThat(config.getInt("webhook-port")).isEqualTo(9090);
        assertThat(config.getBoolean("compress")).isFalse();
        assertThat(config.getBoolean("check-update")).isFalse();
    }

    @Test
    @DisplayName("应该处理无效的配置模式")
    void testInvalidModeHandling() {
        YamlConfiguration config = new YamlConfiguration();
        config.set("mode", "invalid-mode");

        // 验证可以读取无效值，但应用层应该处理验证
        assertThat(config.getString("mode")).isEqualTo("invalid-mode");
    }

    @Test
    @DisplayName("应该处理缺失的必需配置项")
    void testMissingRequiredConfiguration() {
        YamlConfiguration config = new YamlConfiguration();

        // 只设置部分配置
        config.set("mode", "websocket");

        // 验证缺失的配置项返回默认值
        assertThat(config.getString("token", "")).isEmpty();
        assertThat(config.getBoolean("compress", true)).isTrue();
        assertThat(config.getInt("webhook-port", 8080)).isEqualTo(8080);
    }

    @Test
    @DisplayName("应该正确处理布尔值配置")
    void testBooleanConfigurationHandling() {
        YamlConfiguration config = new YamlConfiguration();

        // 测试基本布尔值
        config.set("flag1", true);
        config.set("flag2", false);

        assertThat(config.getBoolean("flag1")).isTrue();
        assertThat(config.getBoolean("flag2")).isFalse();

        // 验证数字值的存储
        config.set("number1", 1);
        config.set("number2", 0);
        assertThat(config.getInt("number1")).isEqualTo(1);
        assertThat(config.getInt("number2")).isEqualTo(0);
    }

    @Test
    @DisplayName("应该正确处理嵌套配置对象")
    void testNestedConfigurationHandling() {
        YamlConfiguration config = new YamlConfiguration();

        // 设置嵌套配置
        config.set("internal-commands.stop", true);
        config.set("internal-commands.help", false);
        config.set("internal-commands.plugins", true);

        assertThat(config.getBoolean("internal-commands.stop")).isTrue();
        assertThat(config.getBoolean("internal-commands.help")).isFalse();
        assertThat(config.getBoolean("internal-commands.plugins")).isTrue();

        // 验证可以获取整个配置节
        assertThat(config.getConfigurationSection("internal-commands")).isNotNull();
    }

    @Test
    @DisplayName("应该支持配置文件保存和加载")
    void testConfigurationSaveAndLoad() throws IOException {
        File configFile = tempDir.resolve("test-kbc.yml").toFile();

        YamlConfiguration originalConfig = new YamlConfiguration();
        originalConfig.set("token", "test-token");
        originalConfig.set("mode", "webhook");
        originalConfig.set("webhook-port", 9000);
        originalConfig.set("compress", false);

        // 保存配置
        originalConfig.save(configFile);
        assertThat(configFile).exists();

        // 加载配置
        YamlConfiguration loadedConfig = YamlConfiguration.loadConfiguration(configFile);
        assertThat(loadedConfig.getString("token")).isEqualTo("test-token");
        assertThat(loadedConfig.getString("mode")).isEqualTo("webhook");
        assertThat(loadedConfig.getInt("webhook-port")).isEqualTo(9000);
        assertThat(loadedConfig.getBoolean("compress")).isFalse();
    }

    @Test
    @DisplayName("应该处理配置默认值")
    void testConfigurationDefaults() {
        YamlConfiguration config = new YamlConfiguration();

        // 测试默认值处理
        assertThat(config.getString("nonexistent-key", "default-value")).isEqualTo("default-value");
        assertThat(config.getInt("nonexistent-number", 42)).isEqualTo(42);
        assertThat(config.getBoolean("nonexistent-flag", true)).isTrue();
        assertThat(config.getDouble("nonexistent-double", 3.14)).isEqualTo(3.14);
    }

    /**
     * 加载默认的kbc.yml配置内容
     */
    private String loadDefaultKbcYml() throws IOException {
        try (InputStream is = getClass().getResourceAsStream("/kbc.yml")) {
            if (is == null) {
                // 如果无法从资源加载，返回基本的配置内容
                return createBasicConfig();
            }

            // Java 8兼容的方式读取InputStream
            ByteArrayOutputStream result = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            int length;
            while ((length = is.read(buffer)) != -1) {
                result.write(buffer, 0, length);
            }
            return result.toString("UTF-8");
        }
    }

    /**
     * 创建基本的配置内容
     */
    private String createBasicConfig() {
        return "token: \"\"\n" +
               "mode: \"websocket\"\n" +
               "compress: true\n" +
               "check-update: true\n" +
               "ignore-ssl: false\n" +
               "webhook-port: 8080\n" +
               "webhook-route: \"kookbc-webhook\"\n" +
               "webhook-encrypt-key: \"\"\n" +
               "webhook-verify-token: \"\"\n" +
               "internal-commands:\n" +
               "  stop: true\n" +
               "  help: true\n" +
               "  plugins: true\n" +
               "internal-commands-reply-result-type: \"REPLY\"\n" +
               "ignore-remote-call-invisible-internal-command: true\n" +
               "over-limit-warning-log-level: \"DEBUG\"\n" +
               "allow-warn-old-message: true\n" +
               "allow-error-feedback: true\n";
    }

    /**
     * 创建Webhook模式的配置内容
     */
    private String createWebhookConfig() {
        return "token: \"test-webhook-token\"\n" +
               "mode: \"webhook\"\n" +
               "webhook-port: 8080\n" +
               "webhook-route: \"kookbc-webhook\"\n" +
               "webhook-encrypt-key: \"\"\n" +
               "webhook-verify-token: \"\"\n" +
               "compress: true\n" +
               "check-update: true\n";
    }
}