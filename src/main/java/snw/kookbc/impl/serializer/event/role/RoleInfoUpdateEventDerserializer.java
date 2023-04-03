package snw.kookbc.impl.serializer.event.role;

import java.lang.reflect.Type;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

import snw.jkook.entity.Guild;
import snw.jkook.event.role.RoleInfoUpdateEvent;
import snw.kookbc.impl.KBCClient;
import snw.kookbc.impl.serializer.event.NormalEventDeserializer;
import snw.kookbc.util.GsonUtil;

public class RoleInfoUpdateEventDerserializer extends NormalEventDeserializer<RoleInfoUpdateEvent> {
    public RoleInfoUpdateEventDerserializer(KBCClient client) {
        super(client);
    }

    @Override
    protected RoleInfoUpdateEvent deserialize(JsonObject object, Type type, JsonDeserializationContext ctx, long timeStamp, JsonObject body) throws JsonParseException {
        Guild guild = client.getStorage().getGuild(GsonUtil.get(object, "target_id").getAsString());
        client.getEntityUpdater().updateRole(
                body,
                client.getStorage().getRole(guild, body.get("role_id").getAsInt(), body)
        );
        return new RoleInfoUpdateEvent(timeStamp, client.getStorage().getRole(guild, body.get("role_id").getAsInt()));
    }
}
