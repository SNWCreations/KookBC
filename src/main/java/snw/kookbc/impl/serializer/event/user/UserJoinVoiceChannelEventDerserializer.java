package snw.kookbc.impl.serializer.event.user;

import java.lang.reflect.Type;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

import snw.jkook.entity.channel.VoiceChannel;
import snw.jkook.event.user.UserJoinVoiceChannelEvent;
import snw.kookbc.impl.KBCClient;
import snw.kookbc.impl.serializer.event.NormalEventDeserializer;

public class UserJoinVoiceChannelEventDerserializer extends NormalEventDeserializer<UserJoinVoiceChannelEvent> {
    public UserJoinVoiceChannelEventDerserializer(KBCClient client) {
        super(client);
    }

    @Override
    protected UserJoinVoiceChannelEvent deserialize(JsonObject object, Type type, JsonDeserializationContext ctx, long timeStamp, JsonObject body) throws JsonParseException {
        return new UserJoinVoiceChannelEvent(
                timeStamp,
                client.getStorage().getUser(body.get("user_id").getAsString()),
                (VoiceChannel) client.getStorage().getChannel(body.get("channel_id").getAsString())
        );
    }
}
