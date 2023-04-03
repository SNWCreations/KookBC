package snw.kookbc.impl.serializer.event.role;

import java.lang.reflect.Type;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

import snw.jkook.entity.Role;
import snw.jkook.event.role.RoleCreateEvent;
import snw.kookbc.impl.KBCClient;
import snw.kookbc.impl.serializer.event.NormalEventDeserializer;
import snw.kookbc.util.GsonUtil;

public class RoleCreateEventDerserializer extends NormalEventDeserializer<RoleCreateEvent> {
    public RoleCreateEventDerserializer(KBCClient client) {
        super(client);
    }

    @Override
    protected RoleCreateEvent deserialize(JsonObject object, Type type, JsonDeserializationContext ctx, long timeStamp, JsonObject body) throws JsonParseException {
        Role role = client.getEntityBuilder().buildRole(
                client.getStorage().getGuild(GsonUtil.get(object, "target_id").getAsString()),
                body
        );
        return new RoleCreateEvent(timeStamp, role);
    }
}
