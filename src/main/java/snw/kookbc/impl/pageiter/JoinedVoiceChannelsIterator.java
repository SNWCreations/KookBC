package snw.kookbc.impl.pageiter;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import snw.jkook.entity.channel.VoiceChannel;
import snw.kookbc.impl.KBCClient;
import snw.kookbc.impl.network.HttpAPIRoute;

import java.util.ArrayList;
import java.util.Collection;

import static snw.kookbc.util.GsonUtil.get;

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
            final String id = get(asObj, "id").getAsString();
            final VoiceChannel channel = (VoiceChannel) client.getStorage().getChannel(id);
            object.add(channel);
        }
    }
}
