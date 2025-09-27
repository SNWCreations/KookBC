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
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import snw.jkook.entity.Game;
import snw.kookbc.impl.KBCClient;
import snw.kookbc.impl.entity.GameImpl;
import snw.kookbc.impl.network.HttpAPIRoute;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

public class GameIterator extends PageIteratorImpl<Collection<Game>> {
    protected final int type;

    public GameIterator(KBCClient client) {
        this(client, 0);
    }

    public GameIterator(KBCClient client, int type) {
        super(client);
        this.type = type;
    }

    @Override
    protected String getRequestURL() {
        return HttpAPIRoute.GAME_LIST.toFullURL() + "?type=" + type;
    }

    @Override
    protected void processElements(JsonNode node) {
        object = new ArrayList<>(node.size());

        for (JsonNode element : node) {
            int id = element.get("id").asInt();
            Game game = client.getStorage().getGame(id);
            if (game != null) {
                ((GameImpl) game).update(element);
            } else {
                game = client.getEntityBuilder().buildGame(element);
                client.getStorage().addGame(game);
            }
            object.add(game);
        }
    }

    // 向后兼容保留的重载方法
    @Override
    protected void processElements(JsonArray array) {
        object = new ArrayList<>(array.size());

        for (JsonElement element : array) {
            JsonNode rawObj = snw.kookbc.util.JacksonUtil.parse(element.toString());
            int id = rawObj.get("id").asInt();
            Game game = client.getStorage().getGame(id);
            if (game != null) {
                ((GameImpl) game).update(rawObj);
            } else {
                game = client.getEntityBuilder().buildGame(rawObj);
                client.getStorage().addGame(game);
            }
            object.add(game);
        }
    }

    @Override
    public Collection<Game> next() {
        return Collections.unmodifiableCollection(super.next());
    }

}
