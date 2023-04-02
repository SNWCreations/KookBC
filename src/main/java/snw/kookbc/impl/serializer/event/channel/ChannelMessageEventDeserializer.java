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

package snw.kookbc.impl.serializer.event.channel;

import com.google.gson.*;
import snw.jkook.event.channel.ChannelMessageEvent;
import snw.jkook.message.TextChannelMessage;
import snw.kookbc.impl.KBCClient;

import java.lang.reflect.Type;

public class ChannelMessageEventDeserializer implements JsonDeserializer<ChannelMessageEvent> {
    private final KBCClient client;

    public ChannelMessageEventDeserializer(KBCClient client) {
        this.client = client;
    }

    @Override
    public ChannelMessageEvent deserialize(JsonElement element, Type type, JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {
        JsonObject object = element.getAsJsonObject();
        TextChannelMessage message = client.getMessageBuilder().buildTextChannelMessage(object);
        // client.getStorage().addMessage(message);
        return new ChannelMessageEvent(message.getTimeStamp(), message.getChannel(), message);
    }
}
