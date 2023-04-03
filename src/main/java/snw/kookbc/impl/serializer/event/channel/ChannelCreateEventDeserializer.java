package snw.kookbc.impl.serializer.event.channel;

import java.lang.reflect.Type;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

import snw.jkook.entity.channel.Channel;
import snw.jkook.event.channel.ChannelCreateEvent;
import snw.kookbc.impl.KBCClient;
import snw.kookbc.impl.serializer.event.NormalEventDeserializer;

public class ChannelCreateEventDeserializer extends NormalEventDeserializer<ChannelCreateEvent> {
    public ChannelCreateEventDeserializer(KBCClient client) {
        super(client);
    }

    @Override
    protected ChannelCreateEvent deserialize(JsonObject object, Type type, JsonDeserializationContext ctx, long timeStamp, JsonObject body) throws JsonParseException {
        Channel newChannel = client.getEntityBuilder().buildChannel(body);
        return new ChannelCreateEvent(timeStamp, newChannel);
    }

    @Override
    protected void beforeReturn(ChannelCreateEvent event) {
        client.getStorage().addChannel(event.getChannel());
    }
}
