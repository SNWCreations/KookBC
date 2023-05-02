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

package snw.kookbc.impl.entity.builder;

import com.google.gson.JsonObject;
import snw.jkook.entity.*;
import snw.jkook.entity.channel.Channel;
import snw.kookbc.impl.entity.*;
import snw.kookbc.impl.entity.channel.ChannelImpl;

public class EntityUpdater {

    public static void updateUser(JsonObject object, User user) {
        ((UserImpl) user).update(object);
    }

    public static void updateGuild(JsonObject object, Guild guild) {
        ((GuildImpl) guild).update(object);
    }

    public static void updateChannel(JsonObject object, Channel channel) {
        ((ChannelImpl) channel).update(object);
    }

    public static void updateRole(JsonObject object, Role role) {
        ((RoleImpl) role).update(object);
    }

    public static void updateEmoji(JsonObject object, CustomEmoji emoji) {
        ((CustomEmojiImpl) emoji).update(object);
    }

    public static void updateGame(JsonObject object, Game game) {
        ((GameImpl) game).update(object);
    }
}
