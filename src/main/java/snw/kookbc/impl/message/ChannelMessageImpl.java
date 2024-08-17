package snw.kookbc.impl.message;

import static snw.kookbc.util.GsonUtil.get;
import static snw.kookbc.util.GsonUtil.has;

import java.util.Collections;
import java.util.Map;
import java.util.NoSuchElementException;

import com.google.gson.JsonObject;

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
import snw.kookbc.impl.network.exceptions.BadResponseException;
import snw.kookbc.util.MapBuilder;

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
        final JsonObject object;
        try {
            object = client.getNetworkClient()
                    .get(HttpAPIRoute.CHANNEL_MESSAGE_INFO.toFullURL() + "?msg_id=" + id);
        } catch (BadResponseException e) {
            if (e.getCode() == 40000) {
                throw (NoSuchElementException) // force casting is required because Throwable#initCause return Throwable
                new NoSuchElementException("No message object with provided ID " + id + " found")
                        .initCause(e);
            }
            throw e;
        }
        final BaseComponent component = client.getMessageBuilder().buildComponent(object);
        long timeStamp = get(object, "create_at").getAsLong();
        ChannelMessage quote = null;
        if (has(object, "quote")) {
            final JsonObject rawQuote = get(object, "quote").getAsJsonObject();
            final String quoteId = get(rawQuote, "id").getAsString();
            quote = client.getCore().getHttpAPI().getChannelMessage(quoteId);
        }
        final String channelId = get(object, "channel_id").getAsString();
        final NonCategoryChannel channel = retrieveOwningChannel(channelId);

        this.component = component;
        this.timeStamp = timeStamp;
        this.quote = quote;
        this.channel = channel;

        client.getStorage().addMessage(this);
        this.completed = true;
    }

    protected NonCategoryChannel retrieveOwningChannel(String id) {
        // todo for removal
        // noinspection deprecation
        return (NonCategoryChannel) client.getCore().getHttpAPI().getChannel(id);
    }
}
