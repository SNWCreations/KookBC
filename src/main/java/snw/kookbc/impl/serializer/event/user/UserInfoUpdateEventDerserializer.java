package snw.kookbc.impl.serializer.event.user;

import java.lang.reflect.Type;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

import snw.jkook.event.user.UserInfoUpdateEvent;
import snw.kookbc.impl.KBCClient;
import snw.kookbc.impl.entity.UserImpl;
import snw.kookbc.impl.serializer.event.NormalEventDeserializer;

public class UserInfoUpdateEventDerserializer extends NormalEventDeserializer<UserInfoUpdateEvent> {
    public UserInfoUpdateEventDerserializer(KBCClient client) {
        super(client);
    }

    @Override
    protected UserInfoUpdateEvent deserialize(JsonObject object, Type type, JsonDeserializationContext ctx, long timeStamp, JsonObject body) throws JsonParseException {
        UserImpl updatedUser = ((UserImpl) client.getStorage().getUser(body.get("body_id").getAsString()));
        updatedUser.setName(body.get("username").getAsString());
        updatedUser.setAvatarUrl(body.get("avatar").getAsString());
        return new UserInfoUpdateEvent(timeStamp, updatedUser);
    }
}
