package snw.kookbc.impl.message;

import snw.jkook.entity.User;
import snw.jkook.entity.channel.NonCategoryChannel;
import snw.jkook.entity.channel.VoiceChannel;
import snw.jkook.message.Message;
import snw.jkook.message.VoiceChannelMessage;
import snw.jkook.message.component.BaseComponent;
import snw.kookbc.impl.KBCClient;

public class VoiceChannelMessageImpl extends ChannelMessageImpl implements VoiceChannelMessage {

    public VoiceChannelMessageImpl(KBCClient client, String id, User user, BaseComponent component, long timeStamp, Message quote, VoiceChannel channel) {
        super(client, id, user, component, timeStamp, quote, channel);
        this.completed = true;
    }

    @Override
    public VoiceChannel getChannel() {
        return (VoiceChannel) super.getChannel();
    }

    @Override
    protected NonCategoryChannel retrieveOwningChannel(String id) {
        return client.getCore().getHttpAPI().getVoiceChannel(id);
    }
}
