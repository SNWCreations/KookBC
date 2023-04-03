package snw.kookbc.impl.serializer.event.channel;

import java.lang.reflect.Type;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

import snw.jkook.entity.channel.Channel;
import snw.jkook.event.channel.ChannelInfoUpdateEvent;
import snw.kookbc.impl.KBCClient;
import snw.kookbc.impl.network.exceptions.BadResponseException;
import snw.kookbc.impl.serializer.event.NormalEventDeserializer;

public class ChannelInfoUpdateEventDeserializer extends NormalEventDeserializer<ChannelInfoUpdateEvent> {
    public ChannelInfoUpdateEventDeserializer(KBCClient client) {
        super(client);
    }

    @Override
    protected ChannelInfoUpdateEvent deserialize(JsonObject object, Type type, JsonDeserializationContext ctx, long timeStamp, JsonObject body) throws JsonParseException {
        Channel channel;
        try {
            channel = client.getStorage().getChannel(body.get("id").getAsString());
        } catch (BadResponseException e) {
            client.getCore().getLogger().warn(
                    "Detected snw.jkook.event.channel.ChannelInfoUpdateEvent, but we are unable to fetch channel (id {}).",
                    body.get("id").getAsString());
            return null;
        }
        client.getEntityUpdater().updateChannel(body, channel);
        return new ChannelInfoUpdateEvent(timeStamp, channel);
    }
}
