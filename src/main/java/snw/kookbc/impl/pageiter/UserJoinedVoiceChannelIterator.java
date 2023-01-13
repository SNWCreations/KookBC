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
import snw.jkook.entity.Guild;
import snw.jkook.entity.User;
import snw.jkook.entity.channel.VoiceChannel;
import snw.kookbc.impl.KBCClient;
import snw.kookbc.impl.network.HttpAPIRoute;

import java.util.Collection;
import java.util.HashSet;

public class UserJoinedVoiceChannelIterator extends PageIteratorImpl<Collection<VoiceChannel>> {
    private final User user;
    private final Guild guild;

    public UserJoinedVoiceChannelIterator(KBCClient client, User user, Guild guild) {
        super(client);
        this.user = user;
        this.guild = guild;
    }

    @Override
    protected String getRequestURL() {
        return String.format("%s?user_id=%s&guild_id=%s", HttpAPIRoute.CHANNEL_USER_VOICE_CHANNEL.toFullURL(), user.getId(), guild.getId());
    }

    @Override
    protected void processElements(JsonArray array) {
        object = new HashSet<>();
        for (JsonElement element : array) {
            object.add((VoiceChannel) client.getStorage().getChannel(element.getAsJsonObject().get("id").getAsString()));
        }
    }
}
