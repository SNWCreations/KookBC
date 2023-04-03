package snw.kookbc.impl.serializer.event.channel;

import java.lang.reflect.Type;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

import snw.jkook.event.channel.ChannelMessageUnpinEvent;
import snw.kookbc.impl.KBCClient;
import snw.kookbc.impl.serializer.event.NormalEventDeserializer;

public class ChannelMessageUnpinEventDeserializer extends NormalEventDeserializer<ChannelMessageUnpinEvent> {
    public ChannelMessageUnpinEventDeserializer(KBCClient client) {
        super(client);
    }

    @Override
    protected ChannelMessageUnpinEvent deserialize(JsonObject object, Type type, JsonDeserializationContext ctx, long timeStamp, JsonObject body) throws JsonParseException {
        return new ChannelMessageUnpinEvent(
                timeStamp,
                client.getStorage().getChannel(body.get("channel_id").getAsString()),
                body.get("msg_id").getAsString(),
                client.getStorage().getUser(body.get("operator_id").getAsString())
        );
    }
}
