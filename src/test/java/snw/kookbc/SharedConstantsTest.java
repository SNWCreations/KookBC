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

package snw.kookbc;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import snw.kookbc.test.BaseTest;

import static org.assertj.core.api.Assertions.*;

/**
 * SharedConstants 测试类
 * 测试 KookBC 的版本信息和常量定义
 */
@DisplayName("SharedConstants 测试")
class SharedConstantsTest extends BaseTest {

    @Test
    @DisplayName("SPEC_NAME 应该被正确初始化")
    void testSpecNameInitialized() {
        assertThat(SharedConstants.SPEC_NAME)
                .isNotNull()
                .isNotEmpty()
                .isEqualTo("JKook");
    }

    @Test
    @DisplayName("SPEC_VERSION 应该被正确初始化")
    void testSpecVersionInitialized() {
        assertThat(SharedConstants.SPEC_VERSION)
                .isNotNull()
                .isNotEmpty()
                .matches("\\d+\\.\\d+\\.\\d+.*"); // 匹配版本号格式 x.y.z
    }

    @Test
    @DisplayName("IMPL_NAME 应该被正确初始化")
    void testImplNameInitialized() {
        assertThat(SharedConstants.IMPL_NAME)
                .isNotNull()
                .isNotEmpty()
                .isEqualTo("KookBC");
    }

    @Test
    @DisplayName("IMPL_VERSION 应该被正确初始化")
    void testImplVersionInitialized() {
        assertThat(SharedConstants.IMPL_VERSION)
                .isNotNull()
                .isNotEmpty()
                .matches("\\d+\\.\\d+\\.\\d+.*"); // 匹配版本号格式 x.y.z
    }

    @Test
    @DisplayName("REPO_URL 应该被正确初始化")
    void testRepoUrlInitialized() {
        assertThat(SharedConstants.REPO_URL)
                .isNotNull()
                .isNotEmpty()
                .startsWith("https://")
                .contains("github.com")
                .contains("KookBC");
    }

    @Test
    @DisplayName("IS_SNAPSHOT 应该被正确初始化为布尔值")
    void testIsSnapshotInitialized() {
        // IS_SNAPSHOT 应该是一个有效的布尔值
        assertThat(SharedConstants.IS_SNAPSHOT).isIn(true, false);
    }

    @Test
    @DisplayName("所有常量都不应该为空")
    void testAllConstantsNotNull() {
        assertThat(SharedConstants.SPEC_NAME).isNotNull();
        assertThat(SharedConstants.SPEC_VERSION).isNotNull();
        assertThat(SharedConstants.IMPL_NAME).isNotNull();
        assertThat(SharedConstants.IMPL_VERSION).isNotNull();
        assertThat(SharedConstants.REPO_URL).isNotNull();
    }

    @Test
    @DisplayName("所有字符串常量都不应该为空字符串")
    void testAllStringConstantsNotEmpty() {
        assertThat(SharedConstants.SPEC_NAME).isNotEmpty();
        assertThat(SharedConstants.SPEC_VERSION).isNotEmpty();
        assertThat(SharedConstants.IMPL_NAME).isNotEmpty();
        assertThat(SharedConstants.IMPL_VERSION).isNotEmpty();
        assertThat(SharedConstants.REPO_URL).isNotEmpty();
    }

    @Test
    @DisplayName("版本字符串格式应该符合语义化版本规范")
    void testVersionFormats() {
        // 检查 JKook API 版本格式
        assertThat(SharedConstants.SPEC_VERSION)
                .matches("\\d+\\.\\d+\\.\\d+(-.+)?");

        // 检查 KookBC 版本格式
        assertThat(SharedConstants.IMPL_VERSION)
                .matches("\\d+\\.\\d+\\.\\d+(-.+)?");
    }

    @Test
    @DisplayName("仓库 URL 应该是有效的 GitHub 链接")
    void testRepositoryUrl() {
        assertThat(SharedConstants.REPO_URL)
                .startsWith("https://github.com/")
                .endsWith("KookBC")
                .doesNotContain(" ")
                .doesNotEndWith("/");
    }

    @Test
    @DisplayName("快照版本标识应该与版本字符串一致")
    void testSnapshotConsistency() {
        if (SharedConstants.IS_SNAPSHOT) {
            assertThat(SharedConstants.IMPL_VERSION)
                    .containsIgnoringCase("SNAPSHOT")
                    .describedAs("如果 IS_SNAPSHOT 为 true，版本字符串应该包含 SNAPSHOT");
        } else {
            assertThat(SharedConstants.IMPL_VERSION)
                    .doesNotContainIgnoringCase("SNAPSHOT")
                    .describedAs("如果 IS_SNAPSHOT 为 false，版本字符串不应该包含 SNAPSHOT");
        }
    }

    @Test
    @DisplayName("常量值应该在多次访问时保持一致")
    void testConstantsConsistency() {
        // 多次访问同一常量，值应该保持一致
        String specName1 = SharedConstants.SPEC_NAME;
        String specName2 = SharedConstants.SPEC_NAME;
        assertThat(specName1).isEqualTo(specName2);

        String implVersion1 = SharedConstants.IMPL_VERSION;
        String implVersion2 = SharedConstants.IMPL_VERSION;
        assertThat(implVersion1).isEqualTo(implVersion2);

        boolean isSnapshot1 = SharedConstants.IS_SNAPSHOT;
        boolean isSnapshot2 = SharedConstants.IS_SNAPSHOT;
        assertThat(isSnapshot1).isEqualTo(isSnapshot2);
    }

    @Test
    @DisplayName("规范名称和实现名称应该不同")
    void testSpecAndImplNamesDifferent() {
        assertThat(SharedConstants.SPEC_NAME)
                .isNotEqualTo(SharedConstants.IMPL_NAME)
                .describedAs("JKook API 规范名称和 KookBC 实现名称应该不同");
    }
}