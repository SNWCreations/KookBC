package snw.kookbc.impl.serializer.event.user;

import java.lang.reflect.Type;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

import snw.jkook.event.user.UserOnlineEvent;
import snw.kookbc.impl.KBCClient;
import snw.kookbc.impl.serializer.event.NormalEventDeserializer;

public class UserOnlineEventDerserializer extends NormalEventDeserializer<UserOnlineEvent> {
    public UserOnlineEventDerserializer(KBCClient client) {
        super(client);
    }

    @Override
    protected UserOnlineEvent deserialize(JsonObject object, Type type, JsonDeserializationContext ctx, long timeStamp, JsonObject body) throws JsonParseException {
        return new UserOnlineEvent(
            timeStamp,
            client.getStorage().getUser(body.get("user_id").getAsString())
        );
    }
}
