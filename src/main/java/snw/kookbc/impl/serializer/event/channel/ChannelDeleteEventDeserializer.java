package snw.kookbc.impl.serializer.event.channel;

import java.lang.reflect.Type;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

import snw.jkook.event.channel.ChannelDeleteEvent;
import snw.kookbc.impl.KBCClient;
import snw.kookbc.impl.serializer.event.NormalEventDeserializer;
import snw.kookbc.util.GsonUtil;

public class ChannelDeleteEventDeserializer extends NormalEventDeserializer<ChannelDeleteEvent> {
    public ChannelDeleteEventDeserializer(KBCClient client) {
        super(client);
    }

    @Override
    protected ChannelDeleteEvent deserialize(JsonObject object, Type type, JsonDeserializationContext ctx, long timeStamp, JsonObject body) throws JsonParseException {
        return new ChannelDeleteEvent(
            timeStamp,
            body.get("id").getAsString(),
            client.getStorage().getGuild(GsonUtil.get(object, "target_id").getAsString())
        );
    }

    @Override
    protected void beforeReturn(ChannelDeleteEvent event) {
        client.getStorage().removeChannel(event.getChannelId());
    }
}
