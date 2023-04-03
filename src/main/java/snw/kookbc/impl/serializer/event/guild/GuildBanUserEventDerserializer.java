package snw.kookbc.impl.serializer.event.guild;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

import snw.jkook.entity.User;
import snw.jkook.event.guild.GuildBanUserEvent;
import snw.kookbc.impl.KBCClient;
import snw.kookbc.impl.serializer.event.NormalEventDeserializer;
import snw.kookbc.util.GsonUtil;

public class GuildBanUserEventDerserializer extends NormalEventDeserializer<GuildBanUserEvent> {
    public GuildBanUserEventDerserializer(KBCClient client) {
        super(client);
    }

    @Override
    protected GuildBanUserEvent deserialize(JsonObject object, Type type, JsonDeserializationContext ctx, long timeStamp, JsonObject body) throws JsonParseException {
        List<User> banned = new ArrayList<>();
        body.getAsJsonArray("user_id").forEach(
                IT -> banned.add(client.getStorage().getUser(IT.getAsString()))
        );
        return new GuildBanUserEvent(
            timeStamp,
            client.getStorage().getGuild(GsonUtil.get(object, "target_id").getAsString()),
            banned,
            client.getStorage().getUser(body.get("operator_id").getAsString()),
            body.get("remark").getAsString()
        );
    }
}
