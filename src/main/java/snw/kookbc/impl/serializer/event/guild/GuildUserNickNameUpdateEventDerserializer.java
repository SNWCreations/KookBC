package snw.kookbc.impl.serializer.event.guild;

import java.lang.reflect.Type;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

import snw.jkook.event.guild.GuildUserNickNameUpdateEvent;
import snw.kookbc.impl.KBCClient;
import snw.kookbc.impl.serializer.event.NormalEventDeserializer;
import snw.kookbc.util.GsonUtil;

public class GuildUserNickNameUpdateEventDerserializer extends NormalEventDeserializer<GuildUserNickNameUpdateEvent> {
    public GuildUserNickNameUpdateEventDerserializer(KBCClient client) {
        super(client);
    }

    @Override
    protected GuildUserNickNameUpdateEvent deserialize(JsonObject object, Type type, JsonDeserializationContext ctx, long timeStamp, JsonObject body) throws JsonParseException {
        return new GuildUserNickNameUpdateEvent(
            timeStamp,
            client.getStorage().getGuild(GsonUtil.get(object, "target_id").getAsString()),
            client.getStorage().getUser(body.get("user_id").getAsString()),
            body.get("nickname").getAsString()
        );
    }
}
