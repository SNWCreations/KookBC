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

package snw.kookbc.impl.network;

import com.google.gson.JsonObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import snw.jkook.exceptions.BadResponseException;
import snw.kookbc.impl.KBCClient;
import snw.kookbc.test.BaseTest;

import static org.assertj.core.api.Assertions.*;

/**
 * NetworkClient 网络客户端基础测试
 * 测试JSON响应处理和基本功能
 */
@DisplayName("NetworkClient 基础功能测试")
class NetworkClientBasicTest extends BaseTest {

    private NetworkClient networkClient;

    @BeforeEach
    protected void setUp() {
        super.setUp();
        KBCClient testClient = createTestKBCClient("test-bot-token");
        this.networkClient = testClient.getNetworkClient();
    }

    @Test
    @DisplayName("checkResponse 应该正确处理成功响应")
    void testCheckResponseSuccess() {
        JsonObject successResponse = new JsonObject();
        successResponse.addProperty("code", 0);
        successResponse.addProperty("message", "success");

        JsonObject result = networkClient.checkResponse(successResponse);
        assertThat(result).isSameAs(successResponse);
    }

    @Test
    @DisplayName("checkResponse 应该在错误响应时抛出异常")
    void testCheckResponseError() {
        JsonObject errorResponse = new JsonObject();
        errorResponse.addProperty("code", 40000);
        errorResponse.addProperty("message", "Invalid token");

        assertThatThrownBy(() -> networkClient.checkResponse(errorResponse))
                .isInstanceOf(BadResponseException.class)
                .hasMessageContaining("40000")
                .hasMessageContaining("Invalid token");
    }

    @Test
    @DisplayName("checkResponse 应该处理各种错误码")
    void testCheckResponseVariousErrorCodes() {
        int[] errorCodes = {40001, 40002, 40003, 50000, 50001};
        for (int errorCode : errorCodes) {
            JsonObject errorResponse = new JsonObject();
            errorResponse.addProperty("code", errorCode);
            errorResponse.addProperty("message", "Error " + errorCode);

            assertThatThrownBy(() -> networkClient.checkResponse(errorResponse))
                    .isInstanceOf(BadResponseException.class)
                    .hasMessageContaining(String.valueOf(errorCode));
        }
    }

    @Test
    @DisplayName("getTokenWithPrefix 应该返回带前缀的token")
    void testGetTokenWithPrefix() {
        assertThat(networkClient.getTokenWithPrefix()).isEqualTo("Bot test-bot-token");
    }

    @Test
    @DisplayName("getOkHttpClient 应该返回有效的HTTP客户端")
    void testGetOkHttpClient() {
        assertThat(networkClient.getOkHttpClient()).isNotNull();
    }
}