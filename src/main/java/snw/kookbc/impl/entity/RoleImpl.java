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

import snw.jkook.Permission;
import snw.jkook.entity.Guild;
import snw.jkook.entity.Role;
import snw.kookbc.impl.KBCClient;
import snw.kookbc.impl.network.HttpAPIRoute;
import snw.kookbc.util.MapBuilder;

import java.util.Map;

public class RoleImpl implements Role {
    private final KBCClient client;
    private final Guild guild;
    private final int id;
    private int color;
    private int position;
    private int permSum;
    private boolean mentionable;
    private boolean hoist;
    private String name;

    public RoleImpl(KBCClient client, Guild guild, int id, int color, int position, int permSum, boolean mentionable, boolean hoist, String name) {
        this.client = client;
        this.guild = guild;
        this.id = id;
        this.color = color;
        this.position = position;
        this.permSum = permSum;
        this.mentionable = mentionable;
        this.hoist = hoist;
        this.name = name;
    }

    @Override
    public int getId() {
        return id;
    }

    @Override
    public int getColor() {
        return color;
    }

    public void setColor(int color) {
        this.color = color;
    }

    @Override
    public int getPosition() {
        return position;
    }

    public void setPosition(int position) {
        this.position = position;
    }

    @Override
    public boolean isPermissionSet(Permission permission) {
        return Permission.hasPermission(permission, permSum);
    }

    @Override
    public boolean isMentionable() {
        return mentionable;
    }

    @Override
    public void setMentionable(boolean value) {
        Map<String, Object> body = new MapBuilder()
                .put("guild_id", getGuild().getId())
                .put("role_id", getId())
                .put("mentionable", (value ? 1 : 0))
                .build();
        client.getNetworkClient().post(HttpAPIRoute.ROLE_UPDATE.toFullURL(), body);
        this.mentionable = value;
    }

    @Override
    public boolean isHoist() {
        return hoist;
    }

    @Override
    public void setHoist(boolean value) {
        Map<String, Object> body = new MapBuilder()
                .put("guild_id", getGuild().getId())
                .put("role_id", getId())
                .put("hoist", (value ? 1 : 0))
                .build();
        client.getNetworkClient().post(HttpAPIRoute.ROLE_UPDATE.toFullURL(), body);
        this.hoist = value;
    }

    @Override
    public void setPermissions(int permValueSum) {
        Map<String, Object> body = new MapBuilder()
                .put("guild_id", getGuild().getId())
                .put("role_id", getId())
                .put("permissions", permValueSum)
                .build();
        client.getNetworkClient().post(HttpAPIRoute.ROLE_UPDATE.toFullURL(), body);
        this.permSum = permValueSum;
    }

    @Override
    public void delete() {
        Map<String, Object> body = new MapBuilder()
                .put("guild_id", getGuild().getId())
                .put("role_id", getId())
                .build();
        client.getNetworkClient().post(HttpAPIRoute.ROLE_DELETE.toFullURL(), body);

    }

    @Override
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Guild getGuild() {
        return guild;
    }

    public void setPermSum(int permSum) {
        this.permSum = permSum;
    }

    public void setHoist0(boolean hoist) {
        this.hoist = hoist;
    }

    public void setMentionable0(boolean mentionable) {
        this.mentionable = mentionable;
    }
}
