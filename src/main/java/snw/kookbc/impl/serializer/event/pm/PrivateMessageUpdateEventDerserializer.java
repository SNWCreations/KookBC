package snw.kookbc.impl.serializer.event.pm;

import java.lang.reflect.Type;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

import snw.jkook.event.pm.PrivateMessageUpdateEvent;
import snw.kookbc.impl.KBCClient;
import snw.kookbc.impl.serializer.event.NormalEventDeserializer;

public class PrivateMessageUpdateEventDerserializer extends NormalEventDeserializer<PrivateMessageUpdateEvent> {
    public PrivateMessageUpdateEventDerserializer(KBCClient client) {
        super(client);
    }

    @Override
    protected PrivateMessageUpdateEvent deserialize(JsonObject object, Type type, JsonDeserializationContext ctx, long timeStamp, JsonObject body) throws JsonParseException {
        return new PrivateMessageUpdateEvent(timeStamp, body.get("msg_id").getAsString(), body.get("content").getAsString());
    }
}
