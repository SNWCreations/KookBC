/*
 *     KookBC -- The Kook Bot Client & JKook API standard implementation for Java.
 *     Copyright (C) 2022 - 2023 KookBC contributors
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
import snw.jkook.entity.Invitation;
import snw.jkook.entity.User;
import snw.jkook.entity.channel.Category;
import snw.jkook.entity.channel.NonCategoryChannel;
import snw.jkook.message.ChannelMessage;
import snw.jkook.message.Message;
import snw.jkook.message.component.BaseComponent;
import snw.jkook.message.component.MarkdownComponent;
import snw.jkook.util.PageIterator;
import snw.kookbc.impl.KBCClient;
import snw.kookbc.impl.entity.builder.MessageBuilder;
import snw.kookbc.impl.network.HttpAPIRoute;
import snw.kookbc.impl.network.exceptions.BadResponseException;
import snw.kookbc.impl.pageiter.ChannelInvitationIterator;
import snw.kookbc.util.MapBuilder;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

import static snw.kookbc.util.GsonUtil.get;

public abstract class NonCategoryChannelImpl extends ChannelImpl implements NonCategoryChannel {
    private Category parent;
    private int chatLimitTime;

    protected NonCategoryChannelImpl(KBCClient client, String id, User master, Guild guild, boolean permSync, Category parent, String name, Collection<RolePermissionOverwrite> rpo, Collection<UserPermissionOverwrite> upo, int level, int chatLimitTime) {
        super(client, id, master, guild, permSync, name, rpo, upo, level);
        this.chatLimitTime = chatLimitTime;
        this.parent = parent;
    }

    @Override
    public String createInvite(int validSeconds, int validTimes) {
        Map<String, Object> body = new MapBuilder()
                .put("channel_id", getId())
                .put("duration", validSeconds)
                .put("setting_times", validTimes)
                .build();
        JsonObject object = client.getNetworkClient().post(HttpAPIRoute.INVITE_CREATE.toFullURL(), body);
        return get(object, "url").getAsString();
    }


    @Override
    public @Nullable Category getParent() {
        return parent;
    }

    @Override
    public void setParent(Category parent) {
        Map<String, Object> body = new MapBuilder()
                .put("channel_id", getId())
                .put("parent_id", (parent == null) ? 0 : parent.getId())
                .build();
        client.getNetworkClient().post(HttpAPIRoute.CHANNEL_UPDATE.toFullURL(), body);
        setParent0(parent);
    }

    public void setParent0(Category parent) {
        this.parent = parent;
    }

    @Override
    public PageIterator<Set<Invitation>> getInvitations() {
        return new ChannelInvitationIterator(client, this);
    }


    @Override
    public String sendComponent(String message) {
        return sendComponent(new MarkdownComponent(message));
    }

    @Override
    public String sendComponent(String message, @Nullable ChannelMessage quote, @Nullable User tempTarget) {
        return sendComponent(new MarkdownComponent(message), quote, tempTarget);
    }

    @Override
    public String sendComponent(BaseComponent baseComponent) {
        return sendComponent(baseComponent, null, null);
    }

    @Override
    public String sendComponent(BaseComponent component, @Nullable ChannelMessage quote, @Nullable User tempTarget) {
        Object[] result = MessageBuilder.serialize(component);
        Map<String, Object> body = new MapBuilder()
                .put("target_id", getId())
                .put("type", result[0])
                .put("content", result[1])
                .putIfNotNull("quote", quote, Message::getId)
                .putIfNotNull("temp_target_id", tempTarget, User::getId)
                .build();
        try {
            return client.getNetworkClient().post(HttpAPIRoute.CHANNEL_MESSAGE_SEND.toFullURL(), body).get("msg_id").getAsString();
        } catch (BadResponseException e) {
            if ("资源不存在".equals(e.getRawMessage())) {
                // 2023/1/17: special case for the resources that aren't created by Bots.
                // Thanks: Edint386@Github
                throw new IllegalArgumentException("Unable to send component. Is the resource created by Bot?", e);
            } else {
                throw e;
            }
        }
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
        client.getNetworkClient().post(HttpAPIRoute.CHANNEL_UPDATE.toFullURL(), body);
        setChatLimitTime0(chatLimitTime);
    }

    public void setChatLimitTime0(int chatLimitTime) {
        this.chatLimitTime = chatLimitTime;
    }

    @Override
    public synchronized void update(JsonObject data) {
        super.update(data);

        String parentId = get(data, "parent_id").getAsString();
        Category parent = ("".equals(parentId) || "0".equals(parentId)) ? null : (Category) client.getStorage().getChannel(parentId);
        setParent0(parent);
    }
}
