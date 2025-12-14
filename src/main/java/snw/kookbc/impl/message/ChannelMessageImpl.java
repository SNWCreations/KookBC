package snw.kookbc.impl.message;

import com.fasterxml.jackson.databind.JsonNode;
import snw.jkook.entity.CustomEmoji;
import snw.jkook.entity.User;
import snw.jkook.entity.channel.NonCategoryChannel;
import snw.jkook.message.ChannelMessage;
import snw.jkook.message.Message;
import snw.jkook.message.component.BaseComponent;
import snw.jkook.message.component.MarkdownComponent;
import snw.jkook.message.component.TemplateMessage;
import snw.kookbc.impl.KBCClient;
import snw.kookbc.impl.entity.builder.MessageBuilder;
import snw.kookbc.impl.network.HttpAPIRoute;
import snw.kookbc.util.MapBuilder;

import java.util.Collections;
import java.util.Map;

import static snw.kookbc.util.JacksonUtil.*;

public class ChannelMessageImpl extends MessageImpl implements ChannelMessage {

    protected NonCategoryChannel channel;

    public ChannelMessageImpl(KBCClient client, String id) {
        super(client, id);
    }

    public ChannelMessageImpl(KBCClient client, String id, User user, BaseComponent component, long timeStamp,
            Message quote, NonCategoryChannel channel) {
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
                .putIfInstance("template_id", component, TemplateMessage.class, TemplateMessage::getId)
                .build();
        client.getNetworkClient().post(
                HttpAPIRoute.CHANNEL_MESSAGE_UPDATE.toFullURL(),
                body);
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
        client.getNetworkClient().postContent(HttpAPIRoute.CHANNEL_MESSAGE_DELETE.toFullURL(),
                Collections.singletonMap("msg_id", getId()));
    }

    @Override
    public void initialize() {
        final String id = getId();
        final JsonNode object = client.getNetworkClient()
                .get(HttpAPIRoute.CHANNEL_MESSAGE_INFO.toFullURL() + "?msg_id=" + id);
        final BaseComponent component = client.getMessageBuilder().buildComponent(object);
        final long timeStamp = get(object, "create_at").asLong();
        ChannelMessage quote = null;
        if (has(object, "quote")) {
            final JsonNode rawQuote = get(object, "quote");
            final String quoteId = rawQuote.get("id").asText();
            quote = client.getCore().getHttpAPI().getChannelMessage(quoteId);
        }
        final String channelId = get(object, "channel_id").asText();
        final NonCategoryChannel channel = retrieveOwningChannel(channelId);
        this.component = component;
        this.timeStamp = timeStamp;
        this.quote = quote;
        this.channel = channel;
        this.completed = true;
        client.getStorage().addMessage(this);
    }

    protected NonCategoryChannel retrieveOwningChannel(String id) {
        // todo for removal
        // noinspection deprecation
        return (NonCategoryChannel) client.getCore().getHttpAPI().getChannel(id);
    }

}
