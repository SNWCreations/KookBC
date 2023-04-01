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

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import snw.jkook.entity.Game;
import snw.kookbc.impl.KBCClient;
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
    protected void processElements(JsonArray array) {
        object = new ArrayList<>(array.size());

        for (JsonElement element : array) {
            JsonObject rawObj = element.getAsJsonObject();
            int id = rawObj.get("id").getAsInt();
            Game game = client.getStorage().getGame(id);
            if (game != null) {
                client.getEntityUpdater().updateGame(rawObj, game);
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
