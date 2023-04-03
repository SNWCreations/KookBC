package snw.kookbc.impl.serializer.event.user;

import java.lang.reflect.Type;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

import snw.jkook.event.user.UserOfflineEvent;
import snw.kookbc.impl.KBCClient;
import snw.kookbc.impl.serializer.event.NormalEventDeserializer;

public class UserOfflineEventDerserializer extends NormalEventDeserializer<UserOfflineEvent> {
    public UserOfflineEventDerserializer(KBCClient client) {
        super(client);
    }

    @Override
    protected UserOfflineEvent deserialize(JsonObject object, Type type, JsonDeserializationContext ctx, long timeStamp, JsonObject body) throws JsonParseException {
        return new UserOfflineEvent(
            timeStamp,
            client.getStorage().getUser(body.get("user_id").getAsString())
        );
    }
}
