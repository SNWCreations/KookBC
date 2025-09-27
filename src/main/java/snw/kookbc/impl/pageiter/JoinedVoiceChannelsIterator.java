package snw.kookbc.impl.pageiter;

import java.util.ArrayList;
import java.util.Collection;

import com.fasterxml.jackson.databind.JsonNode;
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
    protected void processElements(JsonNode node) {
        object = new ArrayList<>(node.size());
        for (JsonNode element : node) {
            final String id = element.get("id").asText();
            final VoiceChannel channel = new VoiceChannelImpl(client, id);
            object.add(channel);
        }
    }

    /**
     * 向后兼容的Gson版本
     * @deprecated 使用 {@link #processElements(JsonNode)} 获得更好的性能
     */
    @Deprecated
    @Override
    protected void processElements(JsonArray array) {
        object = new ArrayList<>(array.size());
        for (JsonElement element : array) {
            final JsonObject asObj = element.getAsJsonObject();
            final String id = asObj.get("id").getAsString();
            final VoiceChannel channel = new VoiceChannelImpl(client, id);
            object.add(channel);
        }
    }
}
