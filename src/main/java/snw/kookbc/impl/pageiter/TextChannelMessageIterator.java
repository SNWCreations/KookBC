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

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import snw.jkook.entity.User;
import snw.jkook.entity.channel.TextChannel;
import snw.jkook.message.Message;
import snw.jkook.message.TextChannelMessage;
import snw.jkook.message.component.BaseComponent;
import snw.kookbc.impl.KBCClient;
import snw.kookbc.impl.message.TextChannelMessageImpl;
import snw.kookbc.impl.network.HttpAPIRoute;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;

import static snw.kookbc.util.GsonUtil.get;

public class TextChannelMessageIterator extends PageIteratorImpl<Collection<TextChannelMessage>> {
    private final TextChannel channel;
    private final String refer;
    private final boolean isPin;
    private final String queryMode;

    public TextChannelMessageIterator(KBCClient client, TextChannel channel, String refer, boolean isPin, String queryMode) {
        super(client);
        this.channel = channel;
        this.refer = refer;
        this.isPin = isPin;
        this.queryMode = queryMode;
    }

    @Override
    protected String getRequestURL() {
        return String.format("%s?target_id=%s%s&pin=%s&flag=%s&page_size=%s", HttpAPIRoute.CHANNEL_MESSAGE_QUERY.toFullURL(), channel.getId(), ((refer != null) ? String.format("&msg_id=%s", "refer") : ""), (isPin ? 1 : 0), queryMode, getPageSize());
    }

    @Override
    protected void processElements(JsonArray array) {
        object = new HashSet<>(array.size());
        for (JsonElement element : array) {
            object.add(buildMessage(element.getAsJsonObject()));
        }
    }

    @Override
    public Collection<TextChannelMessage> next() {
        return Collections.unmodifiableCollection(super.next());
    }

    private TextChannelMessage buildMessage(JsonObject object) {
        String id = get(object, "id").getAsString();
        Message message = client.getStorage().getMessage(id);
        if (message != null) {
            return (TextChannelMessage) message; // if this throw ClassCastException, then we can know the message ID for pm and text channel message is in the same "space"
        }
        long timeStamp = get(object, "create_at").getAsLong();
        JsonObject authorObj = object.getAsJsonObject("author");
        User author = client.getStorage().getUser(authorObj.get("id").getAsString(), authorObj);
        BaseComponent component = client.getMessageBuilder().buildComponent(object);
        return new TextChannelMessageImpl(
                client,
                id,
                author,
                component,
                timeStamp,
                client.getMessageBuilder().buildQuote(object.getAsJsonObject("quote")),
                channel
        );
    }
}
