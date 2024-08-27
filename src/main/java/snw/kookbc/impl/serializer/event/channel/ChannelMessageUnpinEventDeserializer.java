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

import static snw.kookbc.util.GsonUtil.getAsInt;
import static snw.kookbc.util.GsonUtil.getAsString;

import java.lang.reflect.Type;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

import snw.jkook.entity.User;
import snw.jkook.entity.channel.Channel;
import snw.jkook.event.channel.ChannelMessageUnpinEvent;
import snw.kookbc.impl.KBCClient;
import snw.kookbc.impl.entity.channel.TextChannelImpl;
import snw.kookbc.impl.entity.channel.VoiceChannelImpl;
import snw.kookbc.impl.serializer.event.NormalEventDeserializer;

public class ChannelMessageUnpinEventDeserializer extends NormalEventDeserializer<ChannelMessageUnpinEvent> {

    public ChannelMessageUnpinEventDeserializer(KBCClient client) {
        super(client);
    }

    @Override
    protected ChannelMessageUnpinEvent deserialize(JsonObject object, Type type, JsonDeserializationContext ctx,
            long timeStamp, JsonObject body) throws JsonParseException {
        final String id = getAsString(body, "channel_id");
        final Channel channel = getAsInt(body, "channel_type") == 1
                ? new TextChannelImpl(client, id)
                : new VoiceChannelImpl(client, id);
        final String msgId = getAsString(body, "msg_id");
        final User operator = client.getStorage().getUser(getAsString(body, "operator_id"));
        return new ChannelMessageUnpinEvent(timeStamp, channel, msgId, operator);
    }

}
