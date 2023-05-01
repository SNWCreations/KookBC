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

package snw.kookbc.impl.entity;

import snw.jkook.entity.Guild;
import snw.jkook.entity.Invitation;
import snw.jkook.entity.User;
import snw.jkook.entity.channel.Channel;
import snw.kookbc.impl.KBCClient;

public class InvitationImpl implements Invitation {
    private final KBCClient client;
    private final Guild guild;
    private final Channel channel;
    private final String urlCode;
    private final String url;
    private final User master;

    public InvitationImpl(KBCClient client, Guild guild, Channel channel, String urlCode, String url, User master) {
        this.client = client;
        this.guild = guild;
        this.channel = channel;
        this.urlCode = urlCode;
        this.url = url;
        this.master = master;
    }

    @Override
    public Guild getGuild() {
        return guild;
    }

    @Override
    public Channel getChannel() {
        return channel;
    }

    @Override
    public String getUrlCode() {
        return urlCode;
    }

    @Override
    public String getUrl() {
        return url;
    }

    @Override
    public void delete() {
        client.getCore().getHttpAPI().removeInvite(getUrlCode());
    }

    @Override
    public User getMaster() {
        return master;
    }
}
