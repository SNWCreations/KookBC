package snw.kookbc.impl.serializer.event.channel;

import java.lang.reflect.Type;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

import snw.jkook.entity.channel.TextChannel;
import snw.jkook.event.channel.ChannelMessageDeleteEvent;
import snw.kookbc.impl.KBCClient;
import snw.kookbc.impl.serializer.event.NormalEventDeserializer;

public class ChannelMessageDeleteEventDeserializer extends NormalEventDeserializer<ChannelMessageDeleteEvent> {
    public ChannelMessageDeleteEventDeserializer(KBCClient client) {
        super(client);
    }

    @Override
    protected ChannelMessageDeleteEvent deserialize(JsonObject object, Type type, JsonDeserializationContext ctx, long timeStamp, JsonObject body) throws JsonParseException {
        // client.getStorage().removeMessage(body.get("msg_id").getAsString());
        return new ChannelMessageDeleteEvent(
                timeStamp,
                (TextChannel) client.getStorage().getChannel(body.get("channel_id").getAsString()), // if this error, we can regard it as internal error
                body.get("msg_id").getAsString()
        );
    }

    @Override
    protected void beforeReturn(ChannelMessageDeleteEvent event) {
        client.getStorage().removeMessage(event.getMessageId());
    }
}
