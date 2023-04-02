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

package snw.kookbc.impl.serializer.event.pm;

import com.google.gson.*;
import snw.jkook.event.pm.PrivateMessageReceivedEvent;
import snw.jkook.message.PrivateMessage;
import snw.kookbc.impl.KBCClient;

import java.lang.reflect.Type;

public class PrivateMessageReceivedEventDeserializer implements JsonDeserializer<PrivateMessageReceivedEvent> {
    private final KBCClient client;

    public PrivateMessageReceivedEventDeserializer(KBCClient client) {
        this.client = client;
    }

    @Override
    public PrivateMessageReceivedEvent deserialize(JsonElement element, Type type, JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {
        JsonObject object = element.getAsJsonObject();
        PrivateMessage pm = client.getMessageBuilder().buildPrivateMessage(object);
        // client.getStorage().addMessage(pm);
        return new PrivateMessageReceivedEvent(pm.getTimeStamp(), pm.getSender(), pm);
    }
}
