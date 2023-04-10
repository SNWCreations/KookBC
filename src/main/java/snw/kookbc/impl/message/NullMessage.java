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

/**
 * @author huanmeng_qwq
 */
public class NullMessage implements Message {
    public static final NullMessage INSTANCE = new NullMessage();

    private NullMessage() {
    }

    @Override
    public BaseComponent getComponent() {
        return null;
    }

    @Override
    public void setComponent(BaseComponent baseComponent) {

    }

    @Override
    public void setComponent(String s) {

    }

    @Override
    public String getId() {
        return null;
    }

    @Override
    public @Nullable Message getQuote() {
        return null;
    }

    @Override
    public String reply(String s) {
        return null;
    }

    @Override
    public String sendToSource(String s) {
        return null;
    }

    @Override
    public String reply(BaseComponent baseComponent) {
        return null;
    }

    @Override
    public String sendToSource(BaseComponent baseComponent) {
        return null;
    }

    @Override
    public void delete() {

    }

    @Override
    public Collection<User> getUserByReaction(CustomEmoji customEmoji) throws IllegalStateException {
        return null;
    }

    @Override
    public void sendReaction(CustomEmoji customEmoji) {

    }

    @Override
    public void removeReaction(CustomEmoji customEmoji) {

    }

    @Override
    public User getSender() {
        return null;
    }

    @Override
    public long getTimeStamp() {
        return 0;
    }
}
