package snw.kookbc.impl.serializer.event.guild;

import java.lang.reflect.Type;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

import snw.jkook.entity.CustomEmoji;
import snw.jkook.event.guild.GuildAddEmojiEvent;
import snw.kookbc.impl.KBCClient;
import snw.kookbc.impl.serializer.event.NormalEventDeserializer;

public class GuildAddEmojiEventDerserializer extends NormalEventDeserializer<GuildAddEmojiEvent> {
    public GuildAddEmojiEventDerserializer(KBCClient client) {
        super(client);
    }

    @Override
    protected GuildAddEmojiEvent deserialize(JsonObject object, Type type, JsonDeserializationContext ctx, long timeStamp, JsonObject body) throws JsonParseException {
        CustomEmoji emoji1 = client.getEntityBuilder().buildEmoji(body);
        return new GuildAddEmojiEvent(timeStamp, emoji1.getGuild(), emoji1);
    }
}
