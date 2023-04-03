package snw.kookbc.impl.serializer.event.user;

import java.lang.reflect.Type;
import java.util.Objects;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

import snw.jkook.entity.channel.TextChannel;
import snw.jkook.event.user.UserClickButtonEvent;
import snw.kookbc.impl.KBCClient;
import snw.kookbc.impl.serializer.event.NormalEventDeserializer;

public class UserClickButtonEventDerserializer extends NormalEventDeserializer<UserClickButtonEvent> {
    public UserClickButtonEventDerserializer(KBCClient client) {
        super(client);
    }

    @Override
    protected UserClickButtonEvent deserialize(JsonObject object, Type type, JsonDeserializationContext ctx, long timeStamp, JsonObject body) throws JsonParseException {
        return new UserClickButtonEvent(
                timeStamp,
                client.getStorage().getUser(body.get("user_id").getAsString()),
                body.get("msg_id").getAsString(),
                body.get("value").getAsString(),
                Objects.equals(
                        body.get("user_id").getAsString(),
                        body.get("target_id").getAsString()
                ) ? null : (TextChannel) client.getStorage().getChannel(body.get("target_id").getAsString())
        );
    }
}
