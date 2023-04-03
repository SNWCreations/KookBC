package snw.kookbc.impl.serializer.event.channel;

import java.lang.reflect.Type;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

import snw.jkook.event.channel.ChannelMessagePinEvent;
import snw.kookbc.impl.KBCClient;
import snw.kookbc.impl.serializer.event.NormalEventDeserializer;

public class ChannelMessagePinEventDeserializer extends NormalEventDeserializer<ChannelMessagePinEvent> {
    public ChannelMessagePinEventDeserializer(KBCClient client) {
        super(client);
    }

    @Override
    protected ChannelMessagePinEvent deserialize(JsonObject object, Type type, JsonDeserializationContext ctx, long timeStamp, JsonObject body) throws JsonParseException {
        return new ChannelMessagePinEvent(
                timeStamp,
                client.getStorage().getChannel(body.get("channel_id").getAsString()),
                body.get("msg_id").getAsString(),
                client.getStorage().getUser(body.get("operator_id").getAsString())
        );
    }
}
