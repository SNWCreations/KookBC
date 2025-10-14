package snw.kookbc.impl.pageiter;

import java.util.ArrayList;
import java.util.Collection;

import com.fasterxml.jackson.databind.JsonNode;

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
}
