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

package snw.kookbc.impl.serializer.event.guild;

import java.lang.reflect.Type;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

// Jackson Migration Support
import com.fasterxml.jackson.databind.JsonNode;

import snw.jkook.entity.CustomEmoji;
import snw.jkook.entity.Guild;
import snw.jkook.event.guild.GuildAddEmojiEvent;
import snw.kookbc.impl.KBCClient;
import snw.kookbc.impl.serializer.event.NormalEventDeserializer;

public class GuildAddEmojiEventDeserializer extends NormalEventDeserializer<GuildAddEmojiEvent> {

    public GuildAddEmojiEventDeserializer(KBCClient client) {
        super(client);
    }

    @Override
    protected GuildAddEmojiEvent deserialize(JsonObject object, Type type, JsonDeserializationContext ctx,
            long timeStamp, JsonObject body) throws JsonParseException {
        final CustomEmoji customEmoji = client.getEntityBuilder().buildEmoji(body);
        final Guild guild = customEmoji.getGuild();
        return new GuildAddEmojiEvent(timeStamp, guild, customEmoji);
    }

    // ===== Jackson Migration Support =====

    /**
     * Jackson版本的反序列化方法 - 处理Kook不完整JSON数据
     * 提供更好的null-safe处理
     */
    @Override
    protected GuildAddEmojiEvent deserializeFromNode(JsonNode node) {
        // 暂时使用默认实现，等相关依赖完成Jackson迁移
        return super.deserializeFromNode(node);
    }

    /**
     * 启用Jackson反序列化
     */
    @Override
    protected boolean useJacksonDeserialization() {
        // 暂时返回false，等相关依赖完成Jackson迁移
        return false;
    }

}
