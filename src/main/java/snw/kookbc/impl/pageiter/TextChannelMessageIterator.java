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

package snw.kookbc.impl.pageiter;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import snw.jkook.entity.User;
import snw.jkook.entity.channel.TextChannel;
import snw.jkook.message.TextChannelMessage;
import snw.jkook.message.component.BaseComponent;
import snw.kookbc.impl.KBCClient;
import snw.kookbc.impl.message.TextChannelMessageImpl;
import snw.kookbc.impl.network.HttpAPIRoute;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.NoSuchElementException;

public class TextChannelMessageIterator extends PageIteratorImpl<Collection<TextChannelMessage>> {
    private final TextChannel channel;
    private final String refer;
    private final boolean isPin;
    private final String queryMode;

    private Collection<TextChannelMessage> result = null;

    public TextChannelMessageIterator(TextChannel channel, String refer, boolean isPin, String queryMode) {
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
        result = new HashSet<>();
        for (JsonElement element : array) {
            result.add(buildMessage(element.getAsJsonObject()));
        }
    }

    @Override
    protected void onHasNextButNoMoreElement() {
        result = null;
    }

    @Override
    public Collection<TextChannelMessage> next() {
        if (result == null) {
            throw new NoSuchElementException();
        }
        return Collections.unmodifiableCollection(result);
    }

    private TextChannelMessage buildMessage(JsonObject object) {
        String id = object.get("id").getAsString();
        long timeStamp = object.get("create_at").getAsLong();
        JsonObject authorObj = object.getAsJsonObject("author");
        User author = KBCClient.getInstance().getStorage().getUser(authorObj.get("id").getAsString(), authorObj);
        BaseComponent component = KBCClient.getInstance().getMessageBuilder().buildComponent(object);
        return new TextChannelMessageImpl(
                id,
                author,
                component,
                timeStamp,
                channel
        );
    }
}
