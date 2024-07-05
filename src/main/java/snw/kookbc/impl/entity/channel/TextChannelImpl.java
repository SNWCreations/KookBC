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
import snw.jkook.entity.User;
import snw.jkook.entity.channel.Category;
import snw.jkook.entity.channel.TextChannel;
import snw.jkook.message.TextChannelMessage;
import snw.jkook.util.PageIterator;
import snw.jkook.util.Validate;
import snw.kookbc.impl.KBCClient;
import snw.kookbc.impl.network.HttpAPIRoute;
import snw.kookbc.impl.pageiter.TextChannelMessageIterator;
import snw.kookbc.util.MapBuilder;

import java.util.Collection;
import java.util.Map;
import java.util.Objects;

import static snw.kookbc.util.GsonUtil.get;

public class TextChannelImpl extends NonCategoryChannelImpl implements TextChannel {
    private int chatLimitTime;
    private String topic;

    public TextChannelImpl(KBCClient client, String id) {
        super(client, id);
    }

    public TextChannelImpl(KBCClient client, String id, User master, Guild guild, boolean permSync, Category parent, String name, Collection<RolePermissionOverwrite> rpo, Collection<UserPermissionOverwrite> upo, int level, int chatLimitTime, String topic) {
        super(client, id, master, guild, permSync, parent, name, rpo, upo, level, chatLimitTime);
        this.chatLimitTime = chatLimitTime;
        this.topic = topic;
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
        client.getNetworkClient().post(HttpAPIRoute.CHANNEL_UPDATE.toFullURL(), body);
        setTopic0(topic);
    }

    public void setTopic0(String topic) {
        this.topic = topic;
    }

    @Override
    public PageIterator<Collection<TextChannelMessage>> getMessages(@Nullable String refer, boolean isPin, String queryMode) {
        Validate.isTrue(Objects.equals(queryMode, "before") || Objects.equals(queryMode, "around") || Objects.equals(queryMode, "after"), "Invalid queryMode");
        return new TextChannelMessageIterator(client, this, refer, isPin, queryMode);
    }

    @Override
    public void update(JsonObject data) {
        synchronized (this) {
            super.update(data);
            int chatLimitTime = get(data, "slow_mode").getAsInt();
            String topic = get(data, "topic").getAsString();

            this.chatLimitTime = chatLimitTime;
            this.topic = topic;
        }
    }
}
