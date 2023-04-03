package snw.kookbc.impl.serializer.event.channel;

import java.lang.reflect.Type;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

import snw.jkook.event.channel.ChannelMessageUpdateEvent;
import snw.kookbc.impl.KBCClient;
import snw.kookbc.impl.serializer.event.NormalEventDeserializer;

public class ChannelMessageUpdateEventDeserializer extends NormalEventDeserializer<ChannelMessageUpdateEvent> {
    public ChannelMessageUpdateEventDeserializer(KBCClient client) {
        super(client);
    }

    @Override
    protected ChannelMessageUpdateEvent deserialize(JsonObject object, Type type, JsonDeserializationContext ctx, long timeStamp, JsonObject body) throws JsonParseException {
        return new ChannelMessageUpdateEvent(
                timeStamp,
                client.getStorage().getChannel(body.get("channel_id").getAsString()),
                body.get("msg_id").getAsString(),
                body.get("content").getAsString()
        );
    }
}
