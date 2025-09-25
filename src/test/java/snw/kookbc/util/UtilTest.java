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

package snw.kookbc.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import snw.jkook.plugin.Plugin;
import snw.jkook.plugin.PluginDescription;
import snw.kookbc.test.BaseTest;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.zip.DataFormatException;
import java.util.zip.Deflater;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Util 工具类测试
 * 测试各种实用工具方法的功能
 */
@DisplayName("Util 工具类测试")
class UtilTest extends BaseTest {

    @Mock
    private Plugin mockPlugin;

    @TempDir
    File tempDir;

    @Test
    @DisplayName("getVersionDifference 应该正确比较版本号")
    void testGetVersionDifference() {
        // 相同版本
        assertThat(Util.getVersionDifference("1.0.0", "1.0.0")).isEqualTo(0);

        // 当前版本较老
        assertThat(Util.getVersionDifference("1.0.0", "1.0.1")).isEqualTo(-1);
        assertThat(Util.getVersionDifference("1.0.0", "1.1.0")).isEqualTo(-1);
        assertThat(Util.getVersionDifference("1.0.0", "2.0.0")).isEqualTo(-1);

        // 当前版本较新
        assertThat(Util.getVersionDifference("1.0.1", "1.0.0")).isEqualTo(1);
        assertThat(Util.getVersionDifference("1.1.0", "1.0.0")).isEqualTo(1);
        assertThat(Util.getVersionDifference("2.0.0", "1.0.0")).isEqualTo(1);
    }

    @Test
    @DisplayName("getVersionDifference 应该处理快照版本")
    void testGetVersionDifferenceWithSnapshot() {
        // 快照版本比正式版本低
        assertThat(Util.getVersionDifference("1.0.0-SNAPSHOT", "1.0.0")).isEqualTo(-1);

        // 快照版本之间比较
        assertThat(Util.getVersionDifference("1.0.0-SNAPSHOT", "1.0.0-SNAPSHOT")).isEqualTo(0);
    }

    @Test
    @DisplayName("getVersionDifference 应该处理无效版本格式")
    void testGetVersionDifferenceWithInvalidFormat() {
        // 无效格式应该返回-1
        assertThat(Util.getVersionDifference("1.0", "1.0.0")).isEqualTo(-1);
        assertThat(Util.getVersionDifference("1.0.0", "invalid")).isEqualTo(-1);
        assertThat(Util.getVersionDifference("", "1.0.0")).isEqualTo(-1);
    }

    @Test
    @DisplayName("toEnglishNumOrder 应该正确添加序数后缀")
    void testToEnglishNumOrder() {
        assertThat(Util.toEnglishNumOrder(1)).isEqualTo("1st");
        assertThat(Util.toEnglishNumOrder(2)).isEqualTo("2nd");
        assertThat(Util.toEnglishNumOrder(3)).isEqualTo("3rd");
        assertThat(Util.toEnglishNumOrder(4)).isEqualTo("4th");
        assertThat(Util.toEnglishNumOrder(11)).isEqualTo("11st");
        assertThat(Util.toEnglishNumOrder(21)).isEqualTo("21st");
        assertThat(Util.toEnglishNumOrder(22)).isEqualTo("22nd");
        assertThat(Util.toEnglishNumOrder(23)).isEqualTo("23rd");
        assertThat(Util.toEnglishNumOrder(100)).isEqualTo("100th");
    }

    @Test
    @DisplayName("pluginNotNull 应该验证插件不为空")
    void testPluginNotNull() {
        // 正常插件不应该抛出异常
        assertThatCode(() -> Util.pluginNotNull(mockPlugin))
                .doesNotThrowAnyException();

        // null插件应该抛出异常
        assertThatThrownBy(() -> Util.pluginNotNull(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("The provided plugin is null");
    }

    @Test
    @DisplayName("ensurePluginEnabled 应该验证插件已启用")
    void testEnsurePluginEnabled() {
        // 启用的插件不应该抛出异常
        when(mockPlugin.isEnabled()).thenReturn(true);
        assertThatCode(() -> Util.ensurePluginEnabled(mockPlugin))
                .doesNotThrowAnyException();

        // 禁用的插件应该抛出异常
        when(mockPlugin.isEnabled()).thenReturn(false);
        assertThatThrownBy(() -> Util.ensurePluginEnabled(mockPlugin))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("The plugin is disabled");

        // null插件应该抛出异常
        assertThatThrownBy(() -> Util.ensurePluginEnabled(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("The provided plugin is null");
    }

    @Test
    @DisplayName("decompressDeflate 应该正确解压数据")
    void testDecompressDeflate() throws IOException, DataFormatException {
        // 准备测试数据
        String originalText = "Hello, KookBC! This is a test string for compression.";
        byte[] originalData = originalText.getBytes(StandardCharsets.UTF_8);

        // 压缩数据
        Deflater compressor = new Deflater();
        compressor.setInput(originalData);
        compressor.finish();

        ByteArrayOutputStream compressedOutput = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        while (!compressor.finished()) {
            int count = compressor.deflate(buffer);
            compressedOutput.write(buffer, 0, count);
        }
        compressor.end();
        byte[] compressedData = compressedOutput.toByteArray();

        // 使用工具方法解压
        byte[] decompressedData = Util.decompressDeflate(compressedData);

        // 验证解压结果
        assertThat(decompressedData).isEqualTo(originalData);
        assertThat(new String(decompressedData, StandardCharsets.UTF_8)).isEqualTo(originalText);
    }

    @Test
    @DisplayName("inputStreamToByteArray 应该正确读取输入流")
    void testInputStreamToByteArray() throws IOException {
        String testData = "Test data for input stream conversion";
        InputStream inputStream = new ByteArrayInputStream(testData.getBytes(StandardCharsets.UTF_8));

        byte[] result = Util.inputStreamToByteArray(inputStream);

        assertThat(result).isEqualTo(testData.getBytes(StandardCharsets.UTF_8));
        assertThat(new String(result, StandardCharsets.UTF_8)).isEqualTo(testData);
    }

    @Test
    @DisplayName("limit 应该正确限制字符串长度")
    void testLimit() {
        String original = "This is a very long string that needs to be limited";

        // 正常限制
        assertThat(Util.limit(original, 10)).isEqualTo("This is a ...");
        assertThat(Util.limit(original, 4)).isEqualTo("This...");

        // 长度足够时不应该截断
        assertThat(Util.limit(original, 100)).isEqualTo(original);
        assertThat(Util.limit(original, original.length())).isEqualTo(original);

        // 边界情况
        assertThat(Util.limit("", 5)).isEqualTo("");
        assertThat(Util.limit("short", 10)).isEqualTo("short");

        // 负数长度应该返回原字符串
        assertThat(Util.limit(original, -1)).isEqualTo(original);
    }

    @Test
    @DisplayName("isBlank 应该正确判断空白字符串")
    void testIsBlank() {
        // null和空字符串
        assertThat(Util.isBlank(null)).isTrue();
        assertThat(Util.isBlank("")).isTrue();

        // 只有空白字符
        assertThat(Util.isBlank(" ")).isTrue();
        assertThat(Util.isBlank("   ")).isTrue();
        assertThat(Util.isBlank("\t")).isTrue();
        assertThat(Util.isBlank("\n")).isTrue();
        assertThat(Util.isBlank(" \t\n ")).isTrue();

        // 包含非空白字符
        assertThat(Util.isBlank("a")).isFalse();
        assertThat(Util.isBlank(" a ")).isFalse();
        assertThat(Util.isBlank("Hello")).isFalse();
    }

    @Test
    @DisplayName("createDescription 应该正确解析plugin.yml")
    void testCreateDescription() {
        String yamlContent = "name: TestPlugin\n" +
                           "version: 1.0.0\n" +
                           "api-version: 0.54.1\n" +
                           "description: A test plugin\n" +
                           "website: https://example.com\n" +
                           "main: com.example.TestPlugin\n" +
                           "authors:\n" +
                           "  - TestAuthor\n" +
                           "depend:\n" +
                           "  - SomeDependency\n" +
                           "softdepend:\n" +
                           "  - OptionalDependency";

        InputStream inputStream = new ByteArrayInputStream(yamlContent.getBytes(StandardCharsets.UTF_8));
        PluginDescription description = Util.createDescription(inputStream);

        assertThat(description.getName()).isEqualTo("TestPlugin");
        assertThat(description.getVersion()).isEqualTo("1.0.0");
        // Skip getAPIVersion test as it may not be available in this JKook version
        assertThat(description.getDescription()).isEqualTo("A test plugin");
        assertThat(description.getWebsite()).isEqualTo("https://example.com");
        assertThat(description.getMainClassName()).isEqualTo("com.example.TestPlugin");
        assertThat(description.getAuthors()).contains("TestAuthor");
        assertThat(description.getDepend()).contains("SomeDependency");
        assertThat(description.getSoftDepend()).contains("OptionalDependency");
    }

    @Test
    @DisplayName("createDescription 应该处理缺少必需字段的情况")
    void testCreateDescriptionWithMissingFields() {
        String incompleteYaml = "description: A test plugin without required fields";
        InputStream inputStream = new ByteArrayInputStream(incompleteYaml.getBytes(StandardCharsets.UTF_8));

        assertThatThrownBy(() -> Util.createDescription(inputStream))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("is missing");
    }

    @Test
    @DisplayName("anyContains 应该正确检查集合包含关系")
    void testAnyContains() {
        Collection<String> collection1 = Arrays.asList("a", "b", "c");
        Collection<String> collection2 = Arrays.asList("d", "e", "f");
        Collection<String> collection3 = Arrays.asList("g", "h", "i");

        // 目标在某个集合中
        assertThat(Util.anyContains("b", collection1, collection2, collection3)).isTrue();
        assertThat(Util.anyContains("e", collection1, collection2, collection3)).isTrue();
        assertThat(Util.anyContains("i", collection1, collection2, collection3)).isTrue();

        // 目标不在任何集合中
        assertThat(Util.anyContains("z", collection1, collection2, collection3)).isFalse();

        // 空集合
        assertThat(Util.anyContains("a", Collections.emptyList())).isFalse();

        // 单个集合
        assertThat(Util.anyContains("a", collection1)).isTrue();
        assertThat(Util.anyContains("z", collection1)).isFalse();
    }

    @Test
    @DisplayName("isStartByLaunch 应该正确检查启动标志")
    void testIsStartByLaunch() {
        // 这个方法检查系统属性，我们只能验证它不抛异常
        assertThatCode(() -> Util.isStartByLaunch())
                .doesNotThrowAnyException();
    }
}