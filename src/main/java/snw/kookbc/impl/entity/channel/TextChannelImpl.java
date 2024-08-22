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

import static snw.kookbc.util.GsonUtil.getAsInt;
import static snw.kookbc.util.GsonUtil.getAsString;

import java.util.Collection;
import java.util.Map;
import java.util.Objects;

import org.jetbrains.annotations.Nullable;

import com.google.gson.JsonObject;

import snw.jkook.entity.Guild;
import snw.jkook.entity.User;
import snw.jkook.entity.channel.Category;
import snw.jkook.entity.channel.TextChannel;
import snw.jkook.message.ChannelMessage;
import snw.jkook.util.PageIterator;
import snw.jkook.util.Validate;
import snw.kookbc.impl.KBCClient;
import snw.kookbc.impl.network.HttpAPIRoute;
import snw.kookbc.impl.pageiter.ChannelMessageIterator;
import snw.kookbc.util.MapBuilder;

public class TextChannelImpl extends NonCategoryChannelImpl implements TextChannel {
    private int chatLimitTime;
    private String topic;

    public TextChannelImpl(KBCClient client, String id) {
        super(client, id);
    }

    public TextChannelImpl(KBCClient client, String id, User master, Guild guild, boolean permSync, Category parent,
            String name, Collection<RolePermissionOverwrite> rpo, Collection<UserPermissionOverwrite> upo, int level,
            int chatLimitTime, String topic) {
        super(client, id, master, guild, permSync, parent, name, rpo, upo, level, chatLimitTime);
        this.chatLimitTime = chatLimitTime;
        this.topic = topic;
        this.completed = true;
    }

    @Override
    public String getTopic() {
        lazyload();
        return topic;
    }

    @Override
    public void setTopic(String topic) {
        Map<String, Object> body = new MapBuilder()
                .put("channel_id", getId())
                .put("topic", topic)
                .build();
        client.getNetworkClient().post(HttpAPIRoute.CHANNEL_UPDATE.toFullURL(), body);
        this.topic = topic;
    }

    public int getChatLimitTime() {
        return chatLimitTime;
    }

    public void setChatLimitTime(int chatLimitTime) {
        this.chatLimitTime = chatLimitTime;
    }

    @Override
    public PageIterator<Collection<ChannelMessage>> getMessages(@Nullable String refer, boolean isPin,
            String queryMode) {
        Validate.isTrue(Objects.equals(queryMode, "before") || Objects.equals(queryMode, "around")
                || Objects.equals(queryMode, "after"), "Invalid queryMode");
        return new ChannelMessageIterator(client, this, refer, isPin, queryMode);
    }

    @Override
    public synchronized void update(JsonObject data) {
        super.update(data);
        this.chatLimitTime = getAsInt(data, "slow_mode");
        this.topic = getAsString(data, "topic");
    }
}
