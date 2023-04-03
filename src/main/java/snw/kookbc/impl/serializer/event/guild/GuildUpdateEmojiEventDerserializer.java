package snw.kookbc.impl.serializer.event.guild;

import java.lang.reflect.Type;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

import snw.jkook.entity.CustomEmoji;
import snw.jkook.event.guild.GuildUpdateEmojiEvent;
import snw.kookbc.impl.KBCClient;
import snw.kookbc.impl.serializer.event.NormalEventDeserializer;

public class GuildUpdateEmojiEventDerserializer extends NormalEventDeserializer<GuildUpdateEmojiEvent> {
    public GuildUpdateEmojiEventDerserializer(KBCClient client) {
        super(client);
    }

    @Override
    protected GuildUpdateEmojiEvent deserialize(JsonObject object, Type type, JsonDeserializationContext ctx, long timeStamp, JsonObject body) throws JsonParseException {
        CustomEmoji emoji3 = client.getStorage().getEmoji(body.get("id").getAsString(), body);
        client.getEntityUpdater().updateEmoji(body, emoji3);
        return new GuildUpdateEmojiEvent(timeStamp, emoji3.getGuild(), emoji3);
    }
}
