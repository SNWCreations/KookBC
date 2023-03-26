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

package snw.kookbc.impl.event;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

import snw.jkook.event.Event;
import snw.kookbc.impl.KBCClient;

// TODO replace EventFactory class using this class
// after the works on this branch is done.
public class EventFactory2 {
    protected final KBCClient client;
    protected final Gson gson;

    public EventFactory2(KBCClient client) {
        this.client = client;
        this.gson = createGson(this.client);
    }

    public Event createEvent(JsonObject object) {
        return null; // TODO
    }

    protected static Gson createGson(KBCClient client) {
        return null; // TODO
    }
}
