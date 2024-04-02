package snw.kookbc.impl.message;

import snw.jkook.entity.CustomEmoji;
import snw.jkook.entity.User;
import snw.jkook.entity.channel.NonCategoryChannel;
import snw.jkook.message.ChannelMessage;
import snw.jkook.message.Message;
import snw.jkook.message.component.BaseComponent;
import snw.jkook.message.component.MarkdownComponent;
import snw.kookbc.impl.KBCClient;
import snw.kookbc.impl.entity.builder.MessageBuilder;
import snw.kookbc.impl.network.HttpAPIRoute;
import snw.kookbc.util.MapBuilder;

import java.util.Collections;
import java.util.Map;

public class ChannelMessageImpl extends MessageImpl implements ChannelMessage {

    private final NonCategoryChannel channel;

    public ChannelMessageImpl(KBCClient client, String id, User user, BaseComponent component, long timeStamp, Message quote, NonCategoryChannel channel) {
        super(client, id, user, component, timeStamp, quote);
        this.channel = channel;
    }

    @Override
    public void sendReaction(CustomEmoji emoji) {
        Map<String, Object> body = new MapBuilder()
                .put("msg_id", getId())
                .put("emoji", emoji.getId())
                .build();
        client.getNetworkClient().postContent(HttpAPIRoute.CHANNEL_MESSAGE_REACTION_ADD.toFullURL(), body);
    }

    @Override
    public void removeReaction(CustomEmoji emoji) {
        Map<String, Object> body = new MapBuilder()
                .put("msg_id", getId())
                .put("emoji", emoji.getId())
                .build();
        client.getNetworkClient().postContent(HttpAPIRoute.CHANNEL_MESSAGE_REACTION_REMOVE.toFullURL(), body);
    }

    @Override
    public void removeReaction(CustomEmoji emoji, User user) {
        Map<String, Object> body = new MapBuilder()
                .put("msg_id", getId())
                .put("emoji", emoji.getId())
                .put("user_id", user.getId())
                .build();
        client.getNetworkClient().postContent(HttpAPIRoute.CHANNEL_MESSAGE_REACTION_REMOVE.toFullURL(), body);
    }

    @Override
    public NonCategoryChannel getChannel() {
        return channel;
    }

    @Override
    public String replyTemp(String message) {
        return replyTemp(new MarkdownComponent(message));
    }

    @Override
    public String sendToSourceTemp(String message) {
        return sendToSourceTemp(new MarkdownComponent(message));
    }

    @Override
    public String replyTemp(BaseComponent component) {
        return getChannel().sendComponent(component, this, getSender());
    }

    @Override
    public String sendToSourceTemp(BaseComponent component) {
        return getChannel().sendComponent(component, null, getSender());
    }

    @Override
    public void setComponentTemp(User user, BaseComponent component) {
        checkCompatibleComponentType(component);
        Object content = MessageBuilder.serialize(component)[1];
        Map<String, Object> body = new MapBuilder()
                .put("msg_id", getId())
                .put("content", content)
                .put("temp_target_id", user.getId())
                .build();
        client.getNetworkClient().post(
                HttpAPIRoute.CHANNEL_MESSAGE_UPDATE.toFullURL(),
                body
        );
    }

    @Override
    public void setComponentTemp(User user, String s) {
        setComponentTemp(user, new MarkdownComponent(s));
    }

    @Override
    public String reply(String message) {
        return reply(new MarkdownComponent(message));
    }

    @Override
    public String sendToSource(String message) {
        return sendToSource(new MarkdownComponent(message));
    }

    @Override
    public String reply(BaseComponent component) {
        return getChannel().sendComponent(component, this, null);
    }

    @Override
    public String sendToSource(BaseComponent component) {
        return getChannel().sendComponent(component, null, null);
    }

    @Override
    public void delete() {
        client.getNetworkClient().postContent(HttpAPIRoute.CHANNEL_MESSAGE_DELETE.toFullURL(), Collections.singletonMap("msg_id", getId()));
    }
}
