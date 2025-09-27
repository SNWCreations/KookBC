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

package snw.kookbc.impl.pageiter;

import com.fasterxml.jackson.databind.JsonNode;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import snw.jkook.entity.User;
import snw.jkook.entity.channel.TextChannel;
import snw.jkook.message.ChannelMessage;
import snw.jkook.message.Message;
import snw.jkook.message.component.BaseComponent;
import snw.kookbc.impl.KBCClient;
import snw.kookbc.impl.message.ChannelMessageImpl;
import snw.kookbc.impl.network.HttpAPIRoute;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;

public class ChannelMessageIterator extends PageIteratorImpl<Collection<ChannelMessage>> {
    private final TextChannel channel;
    private final String refer;
    private final boolean isPin;
    private final String queryMode;

    public ChannelMessageIterator(KBCClient client, TextChannel channel, String refer, boolean isPin, String queryMode) {
        super(client);
        this.channel = channel;
        this.refer = refer;
        this.isPin = isPin;
        this.queryMode = queryMode;
    }

    @Override
    protected String getRequestURL() {
        return String.format("%s?target_id=%s%s&pin=%s&flag=%s&page_size=%s", HttpAPIRoute.CHANNEL_MESSAGE_QUERY.toFullURL(), channel.getId(), ((refer != null) ? String.format("&msg_id=%s", refer) : ""), (isPin ? 1 : 0), queryMode, getPageSize());
    }

    @Override
    protected void processElements(JsonNode node) {
        object = new LinkedHashSet<>(node.size());
        for (JsonNode element : node) {
            object.add(buildMessage(element));
        }
    }

    // 向后兼容保留的重载方法
    @Override
    protected void processElements(JsonArray array) {
        object = new LinkedHashSet<>(array.size());
        for (JsonElement element : array) {
            object.add(buildMessage(element.getAsJsonObject()));
        }
    }

    @Override
    public Collection<ChannelMessage> next() {
        return Collections.unmodifiableCollection(super.next());
    }

    private ChannelMessage buildMessage(JsonNode node) {
        String id = node.get("id").asText();
        Message message = client.getStorage().getMessage(id);
        if (message != null) {
            return (ChannelMessage) message;
        }
        long timeStamp = node.get("create_at").asLong();
        JsonNode authorNode = node.get("author");
        User author = client.getStorage().getUser(authorNode.get("id").asText(), authorNode);
        BaseComponent component = client.getMessageBuilder().buildComponent(node);
        JsonNode quoteNode = node.get("quote");
        Message quote = quoteNode != null && !quoteNode.isNull() ? client.getMessageBuilder().buildQuote(quoteNode) : null;
        return new ChannelMessageImpl(
                client,
                id,
                author,
                component,
                timeStamp,
                quote,
                channel
        );
    }

    // 向后兼容的Gson版本
    private ChannelMessage buildMessage(JsonObject object) {
        return buildMessage(snw.kookbc.util.JacksonUtil.parse(object.toString()));
    }

}
