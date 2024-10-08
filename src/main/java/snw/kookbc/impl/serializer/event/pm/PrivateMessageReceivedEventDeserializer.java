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

import java.lang.reflect.Type;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

import snw.jkook.entity.User;
import snw.jkook.event.pm.PrivateMessageReceivedEvent;
import snw.jkook.message.PrivateMessage;
import snw.kookbc.impl.KBCClient;
import snw.kookbc.impl.serializer.event.BaseEventDeserializer;

public class PrivateMessageReceivedEventDeserializer extends BaseEventDeserializer<PrivateMessageReceivedEvent> {

    public PrivateMessageReceivedEventDeserializer(KBCClient client) {
        super(client);
    }

    @Override
    protected PrivateMessageReceivedEvent deserialize(JsonObject object, Type type, JsonDeserializationContext ctx)
            throws JsonParseException {
        final PrivateMessage pm = client.getMessageBuilder().buildPrivateMessage(object);
        final long timeStamp = pm.getTimeStamp();
        final User user = pm.getSender();
        return new PrivateMessageReceivedEvent(timeStamp, user, pm);
    }

    @Override
    protected void beforeReturn(PrivateMessageReceivedEvent event) {
        client.getStorage().addMessage(event.getMessage());
    }

}
