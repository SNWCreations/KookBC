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

import com.google.gson.JsonParser;
import okhttp3.FormBody;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import snw.jkook.HttpAPI;
import snw.jkook.bot.Bot;
import snw.jkook.entity.Guild;
import snw.jkook.entity.User;
import snw.jkook.entity.channel.Category;
import snw.jkook.entity.channel.Channel;
import snw.jkook.util.PageIterator;
import snw.kookbc.impl.network.HttpAPIRoute;
import snw.kookbc.impl.pageiter.JoinedGuildIterator;
import snw.kookbc.util.MapBuilder;
import snw.jkook.util.Validate;

import java.io.File;
import java.util.Collection;

public class HttpAPIImpl implements HttpAPI {
    private final KBCClient client;
    private Bot bot;

    public HttpAPIImpl(KBCClient client) {
        this.client = client;
    }

    @Override
    public PageIterator<Collection<Guild>> getJoinedGuilds() {
        return new JoinedGuildIterator();
    }

    @Override
    public User getUser(String s) {
        return client.getStorage().getUser(s);
    }

    @Override
    public Guild getGuild(String s) {
        return client.getStorage().getGuild(s);
    }

    @Override
    public Channel getChannel(String s) {
        return client.getStorage().getChannel(s);
    }

    @Override
    public Category getCategory(String s) {
        try {
            return (Category) client.getStorage().getChannel(s);
        } catch (ClassCastException e) {
            throw new RuntimeException("The object that you requests is not a Category.", e);
        }
    }

    @Override
    public String uploadFile(File file) {
        Request request = new Request.Builder()
                .url(HttpAPIRoute.ASSET_UPLOAD.toFullURL())
                .post(RequestBody.create(file, MediaType.parse("application/from-data")))
                .addHeader("Authorization", String.format("Bot %s", bot.getToken()))
                .build();
        return JsonParser.parseString(client.getNetworkClient().call(request)).getAsJsonObject().getAsJsonObject("data").get("url").getAsString();
    }

    @Override
    public String uploadFile(String binary) {
        Request request = new Request.Builder()
                .url(HttpAPIRoute.ASSET_UPLOAD.toFullURL())
                .post(
                        new FormBody.Builder()
                                .add("file", binary)
                                .build()
                )
                .addHeader("Authorization", String.format("Bot %s", bot.getToken()))
                .build();
        return JsonParser.parseString(client.getNetworkClient().call(request)).getAsJsonObject().getAsJsonObject("data").get("url").getAsString();
    }

    @Override
    public void removeInvite(String urlCode) {
        KBCClient.getInstance().getNetworkClient().post(HttpAPIRoute.INVITE_DELETE.toFullURL(),
                new MapBuilder()
                        .put("url_code", urlCode)
                        .build()
        );
    }

    // Note for developers: PLEASE NEVER CALL THIS METHOD
    public <T extends Bot> void init(T bot) {
        Validate.isTrue(this.bot == null, "This HttpAPI has already initialized.");
        this.bot = bot;
    }
}
