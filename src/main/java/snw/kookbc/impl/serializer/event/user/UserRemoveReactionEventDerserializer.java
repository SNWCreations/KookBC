package snw.kookbc.impl.serializer.event.user;

import java.lang.reflect.Type;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

import snw.jkook.entity.CustomEmoji;
import snw.jkook.entity.Reaction;
import snw.jkook.event.user.UserRemoveReactionEvent;
import snw.kookbc.impl.KBCClient;
import snw.kookbc.impl.entity.ReactionImpl;
import snw.kookbc.impl.serializer.event.NormalEventDeserializer;

public class UserRemoveReactionEventDerserializer extends NormalEventDeserializer<UserRemoveReactionEvent> {
    public UserRemoveReactionEventDerserializer(KBCClient client) {
        super(client);
    }

    @Override
    protected UserRemoveReactionEvent deserialize(JsonObject object, Type type, JsonDeserializationContext ctx, long timeStamp, JsonObject body) throws JsonParseException {
        JsonObject re = body.getAsJsonObject("emoji");
        CustomEmoji em = client.getStorage().getEmoji(re.get("id").getAsString(), re);
        Reaction reaction1 = client.getStorage().getReaction(
                body.get("msg_id").getAsString(), em,
                client.getStorage().getUser(body.get("user_id").getAsString())
        );
        if (reaction1 != null) {
            client.getStorage().removeReaction(reaction1);
        }
        return new UserRemoveReactionEvent(
            timeStamp,
            client.getStorage().getUser(body.get("user_id").getAsString()),
            body.get("msg_id").getAsString(),
            reaction1 == null ? new ReactionImpl(
                client, body.get("msg_id").getAsString(),
                em,
                client.getStorage().getUser(body.get("user_id").getAsString()),
                -1
            ) : reaction1
        );
    }
}
