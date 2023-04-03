package snw.kookbc.impl.serializer.event.user;

import java.lang.reflect.Type;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

import snw.jkook.event.user.UserJoinGuildEvent;
import snw.kookbc.impl.KBCClient;
import snw.kookbc.impl.serializer.event.NormalEventDeserializer;

public class UserJoinGuildEventDerserializer extends NormalEventDeserializer<UserJoinGuildEvent> {
    public UserJoinGuildEventDerserializer(KBCClient client) {
        super(client);
    }

    @Override
    protected UserJoinGuildEvent deserialize(JsonObject object, Type type, JsonDeserializationContext ctx, long timeStamp, JsonObject body) throws JsonParseException {
        return new UserJoinGuildEvent(
            timeStamp,
            client.getCore().getUser(),
            client.getStorage().getGuild(body.get("guild_id").getAsString())
        );
    }
}
