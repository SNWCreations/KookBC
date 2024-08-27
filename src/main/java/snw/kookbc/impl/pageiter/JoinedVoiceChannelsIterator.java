package snw.kookbc.impl.pageiter;

import static snw.kookbc.util.GsonUtil.getAsString;

import java.util.ArrayList;
import java.util.Collection;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import snw.jkook.entity.channel.VoiceChannel;
import snw.kookbc.impl.KBCClient;
import snw.kookbc.impl.entity.channel.VoiceChannelImpl;
import snw.kookbc.impl.network.HttpAPIRoute;

public class JoinedVoiceChannelsIterator extends PageIteratorImpl<Collection<VoiceChannel>> {

    public JoinedVoiceChannelsIterator(KBCClient client) {
        super(client);
    }

    @Override
    protected String getRequestURL() {
        return HttpAPIRoute.VOICE_LIST.toFullURL();
    }

    @Override
    protected void processElements(JsonArray array) {
        object = new ArrayList<>(array.size());
        for (JsonElement element : array) {
            final JsonObject asObj = element.getAsJsonObject();
            final String id = getAsString(asObj, "id");
            final VoiceChannel channel = new VoiceChannelImpl(client, id);
            object.add(channel);
        }
    }
}
