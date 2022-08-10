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

package snw.kookbc.impl;

import snw.jkook.Unsafe;
import snw.jkook.entity.CustomEmoji;
import snw.jkook.entity.Game;
import snw.jkook.message.PrivateMessage;
import snw.kookbc.impl.entity.CustomEmojiImpl;
import snw.kookbc.impl.entity.GameImpl;
import snw.kookbc.impl.message.PrivateMessageImpl;

// Wait! Are you sure you want to this? This can cause RESOURCE LEAK! Beware.
// But also, this is useful in some situations.
// (e.g. Some methods that only need the actual instance but they just need the ID)
public class UnsafeImpl implements Unsafe {

    @Override
    public PrivateMessage getPrivateMessage(String id) {
        return new PrivateMessageImpl(id, null, null, -1, null);
    }

    @Override
    public CustomEmoji getEmoji(String id) {
        return new CustomEmojiImpl(id, null, null);
    }

    @Override
    public Game getGame(int id) {
        return new GameImpl(id, null, null);
    }

}
