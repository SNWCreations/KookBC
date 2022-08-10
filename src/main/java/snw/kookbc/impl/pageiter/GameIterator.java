/*
 *     KookBC -- The Kook Bot Client & JKook API standard implementation for Java.
 *     Copyright (C) 2022 KookBC contributors
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
import java.util.NoSuchElementException;

public class GameIterator extends PageIteratorImpl<Collection<Game>> {
    private Collection<Game> result;

    @Override
    protected String getRequestURL() {
        return HttpAPIRoute.GAME_LIST.toFullURL();
    }

    @Override
    protected void processElements(JsonArray array) {
        result = new ArrayList<>();

        for (JsonElement element : array) {
            JsonObject object = element.getAsJsonObject();
            int id = object.get("id").getAsInt();
            Game game = KBCClient.getInstance().getStorage().getGame(id);
            if (game != null) {
                KBCClient.getInstance().getEntityUpdater().updateGame(object, game);
            } else {
                game = KBCClient.getInstance().getEntityBuilder().buildGame(object);
                KBCClient.getInstance().getStorage().addGame(game);
            }
            result.add(game);
        }
    }

    @Override
    protected void onHasNextButNoMoreElement() {
        result = null;
    }

    @Override
    public Collection<Game> next() {
        if (result == null) {
            throw new NoSuchElementException();
        }
        return result;
    }
}
