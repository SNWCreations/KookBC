/*
 *     KookBC -- The Kook Bot Client & JKook API standard implementation for Java.
 *     Copyright (C) 2022 KookBC contributors
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU Affero General Public License as published
 *     by the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU Affero General Public License for more details.
 *
 *     You should have received a copy of the GNU Affero General Public License
 *     along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package snw.kookbc.impl.entity.channel;

import com.google.gson.JsonObject;
import org.jetbrains.annotations.Nullable;
import snw.jkook.entity.Guild;
import snw.jkook.entity.User;
import snw.jkook.entity.channel.Category;
import snw.jkook.entity.channel.TextChannel;
import snw.jkook.message.TextChannelMessage;
import snw.jkook.message.component.BaseComponent;
import snw.jkook.util.PageIterator;
import snw.jkook.util.Validate;
import snw.kookbc.impl.KBCClient;
import snw.kookbc.impl.entity.builder.MessageBuilder;
import snw.kookbc.impl.network.HttpAPIRoute;
import snw.kookbc.impl.pageiter.TextChannelMessageIterator;
import snw.kookbc.util.MapBuilder;

import java.util.Collection;
import java.util.Map;
import java.util.Objects;

public class TextChannelImpl extends ChannelImpl implements TextChannel {
    private int chatLimitTime;
    private String topic;

    public TextChannelImpl(String id, User master, Guild guild, boolean permSync, Category parent, String name, Collection<RolePermissionOverwrite> rpo, Collection<UserPermissionOverwrite> upo, int level, int chatLimitTime, String topic) {
        super(id, master, guild, permSync, parent, name, rpo, upo, level);
        this.chatLimitTime = chatLimitTime;
        this.topic = topic;
    }

    @Override
    public String createInvite(int validSeconds, int validTimes) {
        Map<String, Object> body = new MapBuilder()
                .put("channel_id", getId())
                .put("duration", validSeconds)
                .put("setting_times", validTimes)
                .build();
        JsonObject object = KBCClient.getInstance().getNetworkClient().post(HttpAPIRoute.INVITE_CREATE.toFullURL(), body);
        return object.get("url").getAsString();
    }

    @Override
    public String getTopic() {
        return topic;
    }

    @Override
    public void setTopic(String topic) {
        Map<String, Object> body = new MapBuilder()
                .put("channel_id", getId())
                .put("topic", topic)
                .build();
        KBCClient.getInstance().getNetworkClient().post(HttpAPIRoute.CHANNEL_UPDATE.toFullURL(), body);
        setTopic0(topic);
    }

    public void setTopic0(String topic) {
        this.topic = topic;
    }

    @Override
    public PageIterator<Collection<TextChannelMessage>> getMessages(@Nullable String refer, boolean isPin, String queryMode) {
        Validate.isTrue(Objects.equals(queryMode, "before") || Objects.equals(queryMode, "around") || Objects.equals(queryMode, "after"), "Invalid queryMode");
        return new TextChannelMessageIterator(this, refer, isPin, queryMode);
    }

    @Override
    public String sendComponent(BaseComponent component, @Nullable TextChannelMessage quote, @Nullable User tempTarget) {
        Object[] result = MessageBuilder.serialize(component);
        MapBuilder builder = new MapBuilder()
                .put("target_id", getId())
                .put("type", result[0])
                .put("content", result[1]);
        if (quote != null) {
            builder.put("quote", quote.getId());
        }
        if (tempTarget != null) {
            builder.put("temp_target_id", tempTarget.getId());
        }
        Map<String, Object> body = builder.build();
        return KBCClient.getInstance().getNetworkClient().post(HttpAPIRoute.CHANNEL_MESSAGE_SEND.toFullURL(), body).get("msg_id").getAsString();
    }

    @Override
    public int getChatLimitTime() {
        return chatLimitTime;
    }

    @Override
    public void setChatLimitTime(int chatLimitTime) {
        Map<String, Object> body = new MapBuilder()
                .put("channel_id", getId())
                .put("slow_mode", chatLimitTime)
                .build();
        KBCClient.getInstance().getNetworkClient().post(HttpAPIRoute.CHANNEL_UPDATE.toFullURL(), body);
        setChatLimitTime0(chatLimitTime);
    }

    public void setChatLimitTime0(int chatLimitTime) {
        this.chatLimitTime = chatLimitTime;
    }

}
