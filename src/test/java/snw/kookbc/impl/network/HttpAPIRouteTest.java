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

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import snw.kookbc.test.BaseTest;

import static org.assertj.core.api.Assertions.*;

/**
 * HttpAPIRoute 测试类
 * 测试HTTP API路由的定义和URL构建功能
 */
@DisplayName("HttpAPIRoute HTTP路由测试")
class HttpAPIRouteTest extends BaseTest {

    @Test
    @DisplayName("BASE_URL 应该正确定义")
    void testBaseUrl() {
        assertThat(HttpAPIRoute.BASE_URL.getRoute())
                .isNotNull()
                .isNotEmpty()
                .startsWith("https://")
                .contains("kookapp.cn");
    }

    @Test
    @DisplayName("getRoute 应该返回正确的路由路径")
    void testGetRoute() {
        assertThat(HttpAPIRoute.USER_ME.getRoute()).isEqualTo("/v3/user/me");
        assertThat(HttpAPIRoute.GATEWAY.getRoute()).isEqualTo("/v3/gateway/index");
        assertThat(HttpAPIRoute.GUILD_JOINED_LIST.getRoute()).isEqualTo("/v3/guild/list");
    }

    @Test
    @DisplayName("toFullURL 应该构建完整的URL")
    void testToFullURL() {
        String baseUrl = HttpAPIRoute.BASE_URL.getRoute();

        assertThat(HttpAPIRoute.USER_ME.toFullURL())
                .isEqualTo(baseUrl + "/v3/user/me");

        assertThat(HttpAPIRoute.GATEWAY.toFullURL())
                .isEqualTo(baseUrl + "/v3/gateway/index");

        assertThat(HttpAPIRoute.GUILD_INFO.toFullURL())
                .isEqualTo(baseUrl + "/v3/guild/view");
    }

    @Test
    @DisplayName("BASE_URL的toFullURL应该只返回基础URL")
    void testBaseUrlToFullURL() {
        assertThat(HttpAPIRoute.BASE_URL.toFullURL())
                .isEqualTo(HttpAPIRoute.BASE_URL.getRoute());
    }

    @Test
    @DisplayName("value方法应该根据路由字符串查找对应枚举")
    void testValueMethod() {
        assertThat(HttpAPIRoute.value("/v3/user/me")).isEqualTo(HttpAPIRoute.USER_ME);
        assertThat(HttpAPIRoute.value("/v3/gateway/index")).isEqualTo(HttpAPIRoute.GATEWAY);
        assertThat(HttpAPIRoute.value("/v3/guild/list")).isEqualTo(HttpAPIRoute.GUILD_JOINED_LIST);
    }

    @Test
    @DisplayName("value方法对不存在的路由应该返回null")
    void testValueMethodWithNonExistentRoute() {
        assertThat(HttpAPIRoute.value("/nonexistent/route")).isNull();
        assertThat(HttpAPIRoute.value("")).isNull();
        assertThat(HttpAPIRoute.value(null)).isNull();
    }

    @Test
    @DisplayName("所有路由都应该以正确的API版本开始")
    void testAllRoutesHaveCorrectAPIVersion() {
        for (HttpAPIRoute route : HttpAPIRoute.values()) {
            if (route != HttpAPIRoute.BASE_URL) {
                assertThat(route.getRoute())
                        .describedAs("Route %s should start with /v3/", route.name())
                        .startsWith("/v3/");
            }
        }
    }

    @Test
    @DisplayName("主要功能路由应该存在")
    void testImportantRoutesExist() {
        // 用户相关路由
        assertThat(HttpAPIRoute.USER_ME).isNotNull();
        assertThat(HttpAPIRoute.USER_WHO).isNotNull();

        // 服务器相关路由
        assertThat(HttpAPIRoute.GUILD_JOINED_LIST).isNotNull();
        assertThat(HttpAPIRoute.GUILD_INFO).isNotNull();

        // 频道相关路由
        assertThat(HttpAPIRoute.CHANNEL_LIST).isNotNull();
        assertThat(HttpAPIRoute.CHANNEL_INFO).isNotNull();

        // 消息相关路由
        assertThat(HttpAPIRoute.CHANNEL_MESSAGE_SEND).isNotNull();
        assertThat(HttpAPIRoute.CHANNEL_MESSAGE_QUERY).isNotNull();

        // 网关路由
        assertThat(HttpAPIRoute.GATEWAY).isNotNull();
    }

    @Test
    @DisplayName("路由映射应该是双向的")
    void testRouteMapping() {
        // 测试几个关键路由的双向映射
        HttpAPIRoute[] testRoutes = {
            HttpAPIRoute.USER_ME,
            HttpAPIRoute.GATEWAY,
            HttpAPIRoute.GUILD_JOINED_LIST,
            HttpAPIRoute.CHANNEL_MESSAGE_SEND
        };

        for (HttpAPIRoute route : testRoutes) {
            String routeString = route.getRoute();
            HttpAPIRoute foundRoute = HttpAPIRoute.value(routeString);

            assertThat(foundRoute)
                    .describedAs("Route mapping should be bidirectional for %s", route.name())
                    .isEqualTo(route);
        }
    }

    @Test
    @DisplayName("完整URL应该是有效的HTTP URL格式")
    void testFullURLFormat() {
        HttpAPIRoute[] testRoutes = {
            HttpAPIRoute.USER_ME,
            HttpAPIRoute.GATEWAY,
            HttpAPIRoute.GUILD_INFO
        };

        for (HttpAPIRoute route : testRoutes) {
            String fullUrl = route.toFullURL();

            assertThat(fullUrl)
                    .describedAs("Full URL for %s should be valid HTTP URL", route.name())
                    .startsWith("https://")
                    .contains("kookapp.cn")
                    .contains("/api/v3/");
        }
    }

    @Test
    @DisplayName("路由枚举值的数量应该合理")
    void testRouteCount() {
        HttpAPIRoute[] routes = HttpAPIRoute.values();

        // KookBC应该支持足够多的API路由
        assertThat(routes.length)
                .isGreaterThan(20)  // 至少应该有20个以上的API路由
                .describedAs("Should have reasonable number of API routes");
    }

    @Test
    @DisplayName("所有路由名称应该遵循命名约定")
    void testRouteNamingConvention() {
        for (HttpAPIRoute route : HttpAPIRoute.values()) {
            String name = route.name();

            assertThat(name)
                    .describedAs("Route name %s should follow naming convention", name)
                    .matches("[A-Z][A-Z0-9_]*");  // 大写字母和下划线
        }
    }
}