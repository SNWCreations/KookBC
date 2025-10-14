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
import snw.jkook.entity.Guild;
import snw.kookbc.impl.KBCClient;
import snw.kookbc.impl.network.HttpAPIRoute;
import snw.kookbc.util.JacksonUtil;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;

public class JoinedGuildIterator extends PageIteratorImpl<Collection<Guild>> {

    public JoinedGuildIterator(KBCClient client) {
        super(client);
    }

    @Override
    protected String getRequestURL() {
        return HttpAPIRoute.GUILD_JOINED_LIST.toFullURL();
    }

    @Override
    protected void processElements(JsonNode node) {
        object = new HashSet<>(node.size());
        for (JsonNode element : node) {
            String id = element.get("id").asText();
            // 使用桥接方法将JsonNode转换为JsonObject给Storage使用
            object.add(client.getStorage().getGuild(id, convertToGsonObject(element)));
        }
    }

    /**
     * 桥接方法：将Jackson JsonNode转换为Gson JsonObject
     * 这是为了与Storage层的接口兼容，Storage层仍在使用Gson接口
     */
    private static com.google.gson.JsonObject convertToGsonObject(JsonNode node) {
        return JacksonUtil.convertToGsonJsonObject(node);
    }

    @Override
    public Collection<Guild> next() {
        return Collections.unmodifiableCollection(super.next());
    }
}
