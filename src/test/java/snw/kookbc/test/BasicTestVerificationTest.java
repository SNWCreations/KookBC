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

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

/**
 * 基础测试验证类 - 确保测试环境正常工作
 */
@DisplayName("基础测试验证")
class BasicTestVerificationTest {

    @Test
    @DisplayName("JUnit 5 应该正常工作")
    void testJUnitWorking() {
        assertThat(true).isTrue();
    }

    @Test
    @DisplayName("AssertJ 应该正常工作")
    void testAssertJWorking() {
        String testString = "KookBC Test Framework";
        assertThat(testString)
                .isNotNull()
                .contains("KookBC")
                .contains("Test")
                .hasSizeGreaterThan(10);
    }

    @Test
    @DisplayName("基本数学计算应该正确")
    void testBasicMath() {
        int result = 2 + 2;
        assertThat(result).isEqualTo(4);
    }
}