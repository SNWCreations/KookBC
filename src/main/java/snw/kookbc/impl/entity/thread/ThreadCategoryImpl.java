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

package snw.kookbc.impl.entity.thread;

import static snw.kookbc.util.JacksonUtil.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import com.fasterxml.jackson.databind.JsonNode;

import snw.jkook.entity.channel.Channel;
import snw.jkook.entity.channel.ThreadChannel.ThreadCategory;
import snw.kookbc.impl.KBCClient;
import snw.kookbc.impl.entity.builder.EntityBuildUtil;

/**
 * ThreadCategory 实体实现
 *
 * <p>代表帖子频道中的分类,用于组织和管理帖子
 *
 * @see ThreadCategory
 * @since KookBC 0.33.0
 */
public class ThreadCategoryImpl implements ThreadCategory {

    private final KBCClient client;
    private final String id;
    private String name;
    private int allow;
    private int deny;
    private Collection<Channel.PermissionOverwrite<?>> roles;
    private boolean isDefault;

    /**
     * 从 Jackson JsonNode 构建 ThreadCategory
     *
     * @param client KBCClient 实例
     * @param data JSON 数据节点
     */
    public ThreadCategoryImpl(KBCClient client, JsonNode data) {
        this.client = client;
        this.id = getStringOrDefault(data, "id", "");
        update(data);
    }

    /**
     * 从 JSON 数据更新分类信息
     *
     * @param data JSON 数据节点
     */
    public synchronized void update(JsonNode data) {
        this.name = getStringOrDefault(data, "name", "");
        this.allow = getIntOrDefault(data, "allow", 0);
        this.deny = getIntOrDefault(data, "deny", 0);
        this.isDefault = getBooleanOrDefault(data, "is_default", false);

        // 解析权限覆写列表
        // 使用 EntityBuildUtil 的 Jackson 版本方法解析角色权限覆写和用户权限覆写
        Collection<Channel.PermissionOverwrite<?>> permissionOverwrites = new ArrayList<>();

        // 解析角色权限覆写 (permission_overwrites)
        Collection<Channel.RolePermissionOverwrite> rolePermissions = EntityBuildUtil.parseRPO(data);
        permissionOverwrites.addAll(rolePermissions);

        // 解析用户权限覆写 (permission_users)
        Collection<Channel.UserPermissionOverwrite> userPermissions = EntityBuildUtil.parseUPO(client, data);
        permissionOverwrites.addAll(userPermissions);

        this.roles = Collections.unmodifiableCollection(permissionOverwrites);
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public int getAllow() {
        return allow;
    }

    @Override
    public int getDeny() {
        return deny;
    }

    @Override
    public Collection<Channel.PermissionOverwrite<?>> getRoles() {
        return roles;
    }

    @Override
    public boolean isDefault() {
        return isDefault;
    }

    @Override
    public String toString() {
        return String.format("ThreadCategory{id=%s, name=%s, isDefault=%s}",
                id, name, isDefault);
    }
}
