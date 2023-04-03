package snw.kookbc.impl.serializer.event.role;

import java.lang.reflect.Type;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

import snw.jkook.entity.Role;
import snw.jkook.event.role.RoleDeleteEvent;
import snw.kookbc.impl.KBCClient;
import snw.kookbc.impl.serializer.event.NormalEventDeserializer;
import snw.kookbc.util.GsonUtil;

public class RoleDeleteEventDerserializer extends NormalEventDeserializer<RoleDeleteEvent> {
    public RoleDeleteEventDerserializer(KBCClient client) {
        super(client);
    }

    @Override
    protected RoleDeleteEvent deserialize(JsonObject object, Type type, JsonDeserializationContext ctx, long timeStamp, JsonObject body) throws JsonParseException {
        Role deletedRole = client.getEntityBuilder().buildRole(
                client.getStorage().getGuild(GsonUtil.get(object, "target_id").getAsString()),
                body);
        return new RoleDeleteEvent(timeStamp, deletedRole);
    }
}
