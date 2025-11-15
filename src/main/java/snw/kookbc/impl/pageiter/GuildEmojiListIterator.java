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

package snw.kookbc.impl.pageiter;

import com.fasterxml.jackson.databind.JsonNode;
import snw.jkook.entity.CustomEmoji;
import snw.jkook.entity.Guild;
import snw.kookbc.impl.KBCClient;
import snw.kookbc.impl.network.HttpAPIRoute;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class GuildEmojiListIterator extends PageIteratorImpl<Set<CustomEmoji>> {
    private final Guild guild;

    public GuildEmojiListIterator(KBCClient client, Guild guild) {
        super(client);
        this.guild = guild;
    }

    @Override
    protected String getRequestURL() {
        return String.format("%s?guild_id=%s", HttpAPIRoute.GUILD_EMOJI_LIST.toFullURL(), guild.getId());
    }

    @Override
    protected void processElements(JsonNode node) {
        object = new HashSet<>(node.size());
        for (JsonNode element : node) {
            String id = element.get("id").asText();
            // 使用桥接方法将JsonNode转换为JsonObject给Storage使用
            object.add(client.getStorage().getEmoji(id, convertToGsonObject(element)));
        }
    }

    /**
     * 桥接方法：将Jackson JsonNode转换为Gson JsonObject
     */
    private static com.google.gson.JsonObject convertToGsonObject(JsonNode node) {
        return new com.google.gson.JsonParser().parse(node.toString()).getAsJsonObject();
    }

    @Override
    public Set<CustomEmoji> next() {
        return Collections.unmodifiableSet(super.next());
    }
}
