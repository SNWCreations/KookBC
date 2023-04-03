package snw.kookbc.impl.serializer.event.guild;

import java.lang.reflect.Type;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

import snw.jkook.event.guild.GuildDeleteEvent;
import snw.kookbc.impl.KBCClient;
import snw.kookbc.impl.serializer.event.NormalEventDeserializer;

public class GuildDeleteEventDerserializer extends NormalEventDeserializer<GuildDeleteEvent> {
    public GuildDeleteEventDerserializer(KBCClient client) {
        super(client);
    }

    @Override
    protected GuildDeleteEvent deserialize(JsonObject object, Type type, JsonDeserializationContext ctx, long timeStamp, JsonObject body) throws JsonParseException {
        client.getStorage().removeGuild(body.get("id").getAsString());
        return new GuildDeleteEvent(timeStamp, body.get("id").getAsString());
    }
}
