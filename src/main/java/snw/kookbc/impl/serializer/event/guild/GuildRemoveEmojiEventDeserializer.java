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

import static snw.kookbc.util.GsonUtil.getAsString;

import java.lang.reflect.Type;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

import snw.jkook.entity.CustomEmoji;
import snw.jkook.entity.Guild;
import snw.jkook.event.guild.GuildRemoveEmojiEvent;
import snw.kookbc.impl.KBCClient;
import snw.kookbc.impl.serializer.event.NormalEventDeserializer;

public class GuildRemoveEmojiEventDeserializer extends NormalEventDeserializer<GuildRemoveEmojiEvent> {

    public GuildRemoveEmojiEventDeserializer(KBCClient client) {
        super(client);
    }

    @Override
    protected GuildRemoveEmojiEvent deserialize(JsonObject object, Type type, JsonDeserializationContext ctx,
            long timeStamp, JsonObject body) throws JsonParseException {
        final CustomEmoji customEmoji = client.getStorage().getEmoji(getAsString(body, "id"), body);
        final Guild guild = customEmoji.getGuild();
        return new GuildRemoveEmojiEvent(timeStamp, guild, customEmoji);
    }

    @Override
    protected void beforeReturn(GuildRemoveEmojiEvent event) {
        client.getStorage().removeEmoji(event.getEmoji());
    }
}
