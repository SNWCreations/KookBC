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
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import snw.jkook.event.Event;
import snw.jkook.event.channel.ChannelMessageEvent;
import snw.jkook.event.item.ItemConsumedEvent;
import snw.jkook.event.pm.PrivateMessageReceivedEvent;
import snw.kookbc.impl.KBCClient;
import snw.kookbc.impl.serializer.event.channel.ChannelMessageEventDeserializer;
import snw.kookbc.impl.serializer.event.item.ItemConsumedEventDeserializer;
import snw.kookbc.impl.serializer.event.pm.PrivateMessageReceivedEventDeserializer;

import static snw.kookbc.util.GsonUtil.get;

// TODO replace EventFactory class using this class
// after the works on this branch is done.
public class EventFactory2 {
    protected final KBCClient client;
    protected final Gson gson;

    public EventFactory2(KBCClient client) {
        this.client = client;
        this.gson = createGson();
    }

    public Event createEvent(JsonObject object) {
        return this.gson.fromJson(object, parseEventType(object));
    }

    protected Class<? extends Event> parseEventType(JsonObject object) {
        final String type = get(get(object, "extra").getAsJsonObject(), "type").getAsString();
        if ("12".equals(type)) {
            return ItemConsumedEvent.class;
        }
        if (EventTypeMap.MAP.containsKey(type)) {
            return EventTypeMap.MAP.get(type);
        }
        // must be number at this time?
        if ("PERSON".equals(get(object, "channel_type").getAsString())) {
            return PrivateMessageReceivedEvent.class;
        } else {
            return ChannelMessageEvent.class;
        }
    }

    // NOT static, so it can be override.
    protected Gson createGson() {
        final KBCClient client = this.client;
        return new GsonBuilder()
                // --- UNUSUAL EVENTS START ---
                .registerTypeAdapter(ChannelMessageEvent.class, new ChannelMessageEventDeserializer(client))
                .registerTypeAdapter(ItemConsumedEvent.class, new ItemConsumedEventDeserializer(client))
                .registerTypeAdapter(PrivateMessageReceivedEvent.class, new PrivateMessageReceivedEventDeserializer(client))
                // --- UNUSUAL EVENTS END   ---
                // TODO add type adapters
                .create();
    }
}
