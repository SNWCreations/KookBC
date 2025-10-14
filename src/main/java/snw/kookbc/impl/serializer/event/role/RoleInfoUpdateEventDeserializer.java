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

package snw.kookbc.impl.serializer.event.role;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

// Jackson Migration Support
import com.fasterxml.jackson.databind.JsonNode;
import snw.jkook.entity.Guild;
import snw.jkook.entity.Role;
import snw.jkook.event.role.RoleInfoUpdateEvent;
import snw.kookbc.impl.KBCClient;
import snw.kookbc.impl.entity.RoleImpl;
import snw.kookbc.impl.serializer.event.NormalEventDeserializer;

import java.lang.reflect.Type;


public class RoleInfoUpdateEventDeserializer extends NormalEventDeserializer<RoleInfoUpdateEvent> {

    public RoleInfoUpdateEventDeserializer(KBCClient client) {
        super(client);
    }

    @Override
    protected RoleInfoUpdateEvent deserialize(JsonObject object, Type type, JsonDeserializationContext ctx,
            long timeStamp, JsonObject body) throws JsonParseException {
        final Guild guild = client.getStorage().getGuild(object.get("target_id").getAsString());
        final int roleId = body.get("role_id").getAsInt();
        ((RoleImpl) client.getStorage().getRole(guild, roleId, body)).update(body);
        final Role role = client.getStorage().getRole(guild, roleId);
        return new RoleInfoUpdateEvent(timeStamp, role);
    }

    // ===== Jackson Migration Support =====

    /**
     * Jackson版本的反序列化方法 - 处理Kook不完整JSON数据
     * 提供更好的null-safe处理
     */
    @Override
    protected RoleInfoUpdateEvent deserializeFromNode(JsonNode node, long timeStamp, JsonNode body) {
        final Guild guild = client.getStorage().getGuild(node.get("target_id").asText());
        final int roleId = body.get("role_id").asInt();
        ((RoleImpl) client.getStorage().getRole(guild, roleId, body)).update(body);
        final Role role = client.getStorage().getRole(guild, roleId);
        return new RoleInfoUpdateEvent(timeStamp, role);
    }

    /**
     * 启用Jackson反序列化
     */
    @Override
    protected boolean useJacksonDeserialization() {
        // RoleImpl已支持Jackson update，可以启用
        return true;
    }

}
