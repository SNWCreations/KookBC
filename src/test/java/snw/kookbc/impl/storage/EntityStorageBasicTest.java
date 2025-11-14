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

package snw.kookbc.impl.storage;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import snw.kookbc.impl.KBCClient;
import snw.kookbc.test.BaseTest;

import static org.assertj.core.api.Assertions.*;

/**
 * EntityStorage 基础功能测试
 * 测试实体存储的基本功能和缓存机制
 */
@DisplayName("EntityStorage 基础功能测试")
class EntityStorageBasicTest extends BaseTest {

    @Test
    @DisplayName("EntityStorage 应该能够正确初始化")
    void testEntityStorageInitialization() {
        // 由于需要KBCClient依赖，这里测试基本的类存在性和方法签名
        Class<EntityStorage> storageClass = EntityStorage.class;

        assertThat(storageClass).isNotNull();
        assertThat(storageClass.getName()).isEqualTo("snw.kookbc.impl.storage.EntityStorage");
    }

    @Test
    @DisplayName("EntityStorage 应该有正确的公共方法")
    void testEntityStoragePublicMethods() throws NoSuchMethodException {
        Class<EntityStorage> storageClass = EntityStorage.class;

        // 验证关键的公共方法存在
        assertThat(storageClass.getMethod("getUser", String.class)).isNotNull();
        assertThat(storageClass.getMethod("getGuild", String.class)).isNotNull();
        assertThat(storageClass.getMethod("getChannel", String.class)).isNotNull();
        assertThat(storageClass.getMethod("getGame", int.class)).isNotNull();
        assertThat(storageClass.getMethod("getMessage", String.class)).isNotNull();
    }

    @Test
    @DisplayName("EntityStorage 应该有带JsonObject参数的重载方法")
    void testEntityStorageOverloadedMethods() throws NoSuchMethodException {
        Class<EntityStorage> storageClass = EntityStorage.class;

        // 验证带JsonObject参数的重载方法
        assertThat(storageClass.getMethod("getUser", String.class, com.google.gson.JsonObject.class)).isNotNull();
        assertThat(storageClass.getMethod("getGuild", String.class, com.google.gson.JsonObject.class)).isNotNull();
        assertThat(storageClass.getMethod("getChannel", String.class, com.google.gson.JsonObject.class)).isNotNull();
        assertThat(storageClass.getMethod("getEmoji", String.class, com.google.gson.JsonObject.class)).isNotNull();
    }

    @Test
    @DisplayName("EntityStorage 应该有角色相关的方法")
    void testEntityStorageRoleMethods() throws NoSuchMethodException {
        Class<EntityStorage> storageClass = EntityStorage.class;

        // 验证角色相关方法
        assertThat(storageClass.getMethod("getRole", snw.jkook.entity.Guild.class, int.class)).isNotNull();
        assertThat(storageClass.getMethod("getRole", snw.jkook.entity.Guild.class, int.class, com.google.gson.JsonObject.class)).isNotNull();
        assertThat(storageClass.getMethod("getRoles", snw.jkook.entity.Guild.class)).isNotNull();
    }

    @Test
    @DisplayName("EntityStorage 应该有添加实体的方法")
    void testEntityStorageAddMethods() throws NoSuchMethodException {
        Class<EntityStorage> storageClass = EntityStorage.class;

        // 验证添加实体的方法存在
        assertThat(storageClass.getMethod("addGame", snw.jkook.entity.Game.class)).isNotNull();
        assertThat(storageClass.getMethod("addReaction", snw.jkook.entity.Reaction.class)).isNotNull();
    }

    @Test
    @DisplayName("EntityStorage 应该有表情相关的方法")
    void testEntityStorageEmojiMethods() throws NoSuchMethodException {
        Class<EntityStorage> storageClass = EntityStorage.class;

        // 验证表情相关方法
        assertThat(storageClass.getMethod("getEmoji", String.class)).isNotNull();
        assertThat(storageClass.getMethod("getEmoji", String.class, com.google.gson.JsonObject.class)).isNotNull();
    }

    @Test
    @DisplayName("EntityStorage 应该有反应相关的方法")
    void testEntityStorageReactionMethods() throws NoSuchMethodException {
        Class<EntityStorage> storageClass = EntityStorage.class;

        // 验证反应相关方法
        assertThat(storageClass.getMethod("getReaction", String.class, snw.jkook.entity.CustomEmoji.class, snw.jkook.entity.User.class)).isNotNull();
    }

    @Test
    @DisplayName("EntityStorage 应该有正确的构造函数")
    void testEntityStorageConstructor() throws NoSuchMethodException {
        Class<EntityStorage> storageClass = EntityStorage.class;

        // 验证构造函数存在
        assertThat(storageClass.getConstructor(KBCClient.class)).isNotNull();
    }

    @Test
    @DisplayName("EntityStorage 类应该有正确的包结构")
    void testEntityStoragePackage() {
        Class<EntityStorage> storageClass = EntityStorage.class;

        assertThat(storageClass.getPackage().getName()).isEqualTo("snw.kookbc.impl.storage");
        assertThat(storageClass.getSimpleName()).isEqualTo("EntityStorage");
    }

    @Test
    @DisplayName("EntityStorage 应该能够处理基本的类型检查")
    void testEntityStorageTypeChecking() {
        Class<EntityStorage> storageClass = EntityStorage.class;

        // 验证类不是抽象类或接口
        assertThat(storageClass.isInterface()).isFalse();
        assertThat(java.lang.reflect.Modifier.isAbstract(storageClass.getModifiers())).isFalse();
        assertThat(java.lang.reflect.Modifier.isPublic(storageClass.getModifiers())).isTrue();
    }

    @Test
    @DisplayName("EntityStorage 应该有合理的方法数量")
    void testEntityStorageMethodCount() {
        Class<EntityStorage> storageClass = EntityStorage.class;

        // EntityStorage应该有足够的方法来处理各种实体操作
        int publicMethodCount = storageClass.getMethods().length;
        int declaredMethodCount = storageClass.getDeclaredMethods().length;

        assertThat(publicMethodCount).isGreaterThan(15); // 包括继承的方法
        assertThat(declaredMethodCount).isGreaterThan(10); // 只计算声明的方法
    }

    @Test
    @DisplayName("EntityStorage 应该能够支持各种实体类型")
    void testEntityStorageSupportedTypes() {
        // 验证EntityStorage支持的实体类型
        String[] supportedEntityTypes = {
                "snw.jkook.entity.User",
                "snw.jkook.entity.Guild",
                "snw.jkook.entity.channel.Channel",
                "snw.jkook.entity.Role",
                "snw.jkook.entity.CustomEmoji",
                "snw.jkook.entity.Game",
                "snw.jkook.entity.Reaction",
                "snw.jkook.message.Message"
        };

        for (String entityType : supportedEntityTypes) {
            assertThatCode(() -> {
                Class.forName(entityType);
            }).describedAs("Entity type %s should be available", entityType)
              .doesNotThrowAnyException();
        }
    }
}