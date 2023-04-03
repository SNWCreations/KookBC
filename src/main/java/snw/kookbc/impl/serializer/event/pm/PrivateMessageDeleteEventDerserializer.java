package snw.kookbc.impl.serializer.event.pm;

import java.lang.reflect.Type;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

import snw.jkook.event.pm.PrivateMessageDeleteEvent;
import snw.kookbc.impl.KBCClient;
import snw.kookbc.impl.serializer.event.NormalEventDeserializer;

public class PrivateMessageDeleteEventDerserializer extends NormalEventDeserializer<PrivateMessageDeleteEvent> {
    public PrivateMessageDeleteEventDerserializer(KBCClient client) {
        super(client);
    }

    @Override
    protected PrivateMessageDeleteEvent deserialize(JsonObject object, Type type, JsonDeserializationContext ctx, long timeStamp, JsonObject body) throws JsonParseException {
        client.getStorage().removeMessage(body.get("msg_id").getAsString());
        return new PrivateMessageDeleteEvent(timeStamp, body.get("msg_id").getAsString());
    }
}
