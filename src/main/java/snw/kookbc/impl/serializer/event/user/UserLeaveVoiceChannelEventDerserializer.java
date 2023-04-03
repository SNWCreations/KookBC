package snw.kookbc.impl.serializer.event.user;

import java.lang.reflect.Type;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

import snw.jkook.entity.channel.VoiceChannel;
import snw.jkook.event.user.UserLeaveVoiceChannelEvent;
import snw.kookbc.impl.KBCClient;
import snw.kookbc.impl.serializer.event.NormalEventDeserializer;

public class UserLeaveVoiceChannelEventDerserializer extends NormalEventDeserializer<UserLeaveVoiceChannelEvent> {
    public UserLeaveVoiceChannelEventDerserializer(KBCClient client) {
        super(client);
    }

    @Override
    protected UserLeaveVoiceChannelEvent deserialize(JsonObject object, Type type, JsonDeserializationContext ctx, long timeStamp, JsonObject body) throws JsonParseException {
        return new UserLeaveVoiceChannelEvent(
            timeStamp,
            client.getStorage().getUser(body.get("user_id").getAsString()),
            (VoiceChannel) client.getStorage().getChannel(body.get("channel_id").getAsString())
        );
    }
}
