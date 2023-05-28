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

import org.jetbrains.annotations.Nullable;
import snw.jkook.entity.CustomEmoji;
import snw.jkook.entity.User;
import snw.jkook.message.Message;
import snw.jkook.message.component.BaseComponent;

import java.util.Collection;

// This is a temporary bean object for the situations like quote received, but original object missing.
public class QuoteImpl implements Message {
    private final BaseComponent component;
    private final String id;
    private final User sender;
    private final long timeStamp;

    public QuoteImpl(BaseComponent component, String id, User sender, long timeStamp) {
        this.component = component;
        this.id = id;
        this.sender = sender;
        this.timeStamp = timeStamp;
    }

    @Override
    public BaseComponent getComponent() {
        return component;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public User getSender() {
        return sender;
    }

    @Override
    public long getTimeStamp() {
        return timeStamp;
    }

    // following methods are NOT supported by this implementation!

    @Override
    public void setComponent(BaseComponent component) {
    }

    @Override
    public void setComponent(String s) {
    }

    @Override
    public @Nullable Message getQuote() {
        return null;
    }

    @Override
    public String reply(String message) {
        return null;
    }

    @Override
    public String sendToSource(String message) {
        return null;
    }

    @Override
    public String reply(BaseComponent component) {
        return null;
    }

    @Override
    public String sendToSource(BaseComponent component) {
        return null;
    }

    @Override
    public void delete() {
    }

    @Override
    public Collection<User> getUserByReaction(CustomEmoji emoji) throws IllegalStateException {
        return null;
    }

    @Override
    public void sendReaction(CustomEmoji emoji) {
    }

    @Override
    public void removeReaction(CustomEmoji emoji) {
    }

}
