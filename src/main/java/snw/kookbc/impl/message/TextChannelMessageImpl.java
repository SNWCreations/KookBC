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

package snw.kookbc.impl.message;

import snw.jkook.entity.User;
import snw.jkook.entity.channel.NonCategoryChannel;
import snw.jkook.entity.channel.TextChannel;
import snw.jkook.message.Message;
import snw.jkook.message.TextChannelMessage;
import snw.jkook.message.component.BaseComponent;
import snw.kookbc.impl.KBCClient;

public class TextChannelMessageImpl extends ChannelMessageImpl implements TextChannelMessage {

    public TextChannelMessageImpl(KBCClient client, String id) {
        super(client, id);
    }

    public TextChannelMessageImpl(KBCClient client, String id, User user, BaseComponent component, long timeStamp,
            Message quote, TextChannel channel) {
        super(client, id, user, component, timeStamp, quote, channel);
        this.completed = true;
    }

    @Override
    public TextChannel getChannel() {
        return (TextChannel) super.getChannel();
    }

    @Override
    protected NonCategoryChannel retrieveOwningChannel(String id) {
        return client.getCore().getHttpAPI().getTextChannel(id);
    }

}
