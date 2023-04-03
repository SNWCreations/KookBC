package snw.kookbc.impl.serializer.event.guild;

import java.lang.reflect.Type;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

import snw.jkook.entity.Guild;
import snw.jkook.event.guild.GuildInfoUpdateEvent;
import snw.kookbc.impl.KBCClient;
import snw.kookbc.impl.serializer.event.NormalEventDeserializer;

public class GuildInfoUpdateEventDerserializer extends NormalEventDeserializer<GuildInfoUpdateEvent> {
    public GuildInfoUpdateEventDerserializer(KBCClient client) {
        super(client);
    }

    @Override
    protected GuildInfoUpdateEvent deserialize(JsonObject object, Type type, JsonDeserializationContext ctx, long timeStamp, JsonObject body) throws JsonParseException {
        Guild guild = client.getStorage().getGuild(body.get("id").getAsString());
        client.getEntityUpdater().updateGuild(body, guild);
        return new GuildInfoUpdateEvent(timeStamp, guild);
    }
}
