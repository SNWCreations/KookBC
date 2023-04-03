package snw.kookbc.impl.serializer.event.guild;

import java.lang.reflect.Type;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

import snw.jkook.entity.CustomEmoji;
import snw.jkook.event.guild.GuildRemoveEmojiEvent;
import snw.kookbc.impl.KBCClient;
import snw.kookbc.impl.serializer.event.NormalEventDeserializer;

public class GuildRemoveEmojiEventDerserializer extends NormalEventDeserializer<GuildRemoveEmojiEvent> {
    public GuildRemoveEmojiEventDerserializer(KBCClient client) {
        super(client);
    }

    @Override
    protected GuildRemoveEmojiEvent deserialize(JsonObject object, Type type, JsonDeserializationContext ctx, long timeStamp, JsonObject body) throws JsonParseException {
        CustomEmoji emoji2 = client.getStorage().getEmoji(body.get("id").getAsString(), body);
        return new GuildRemoveEmojiEvent(timeStamp, emoji2.getGuild(), emoji2);
    }
}
