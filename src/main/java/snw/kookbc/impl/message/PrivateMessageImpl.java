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
import snw.jkook.message.Message;
import snw.jkook.message.PrivateMessage;
import snw.jkook.message.component.BaseComponent;
import snw.kookbc.impl.KBCClient;

public class PrivateMessageImpl extends MessageImpl implements PrivateMessage {
    public PrivateMessageImpl(KBCClient client, String id, User user, BaseComponent component, long timeStamp, Message quote) {
        super(client, id, user, component, timeStamp, quote);
    }

    @Override
    public String reply(BaseComponent component) {
        return getSender().sendPrivateMessage(component, this);
    }

    @Override
    public String sendToSource(BaseComponent component) {
        return getSender().sendPrivateMessage(component);
    }

}
