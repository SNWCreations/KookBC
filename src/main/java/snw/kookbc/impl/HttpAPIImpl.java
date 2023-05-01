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

package snw.kookbc.impl;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.Request;
import okhttp3.RequestBody;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import snw.jkook.HttpAPI;
import snw.jkook.entity.Game;
import snw.jkook.entity.Guild;
import snw.jkook.entity.User;
import snw.jkook.entity.channel.Category;
import snw.jkook.entity.channel.Channel;
import snw.jkook.util.PageIterator;
import snw.jkook.util.Validate;
import snw.kookbc.impl.network.HttpAPIRoute;
import snw.kookbc.impl.pageiter.GameIterator;
import snw.kookbc.impl.pageiter.JoinedGuildIterator;
import snw.kookbc.util.MapBuilder;

import java.io.File;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;

public class HttpAPIImpl implements HttpAPI {
    private static final MediaType OCTET_STREAM;
    private static final Collection<String> SUPPORTED_MUSIC_SOFTWARES;

    static {
        OCTET_STREAM = MediaType.parse("application/octet-stream");
        SUPPORTED_MUSIC_SOFTWARES = Collections.unmodifiableCollection(
            Arrays.asList(
                "cloudmusic", "qqmusic", "kugou"
            )
        );
    }

    private final KBCClient client;

    public HttpAPIImpl(KBCClient client) {
        this.client = client;
    }

    @Override
    public PageIterator<Collection<Guild>> getJoinedGuilds() {
        return new JoinedGuildIterator(client);
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
        RequestBody body = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("file", file.getName(), RequestBody.create(file, OCTET_STREAM))
                .build();
        Request request = new Request.Builder()
                .url(HttpAPIRoute.ASSET_UPLOAD.toFullURL())
                .post(body)
                .addHeader("Authorization", client.getNetworkClient().getTokenWithPrefix())
                .build();
        return JsonParser.parseString(client.getNetworkClient().call(request)).getAsJsonObject().getAsJsonObject("data").get("url").getAsString();
    }

    @Override
    public String uploadFile(String filename, byte[] content) {
        RequestBody requestBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("file", filename, RequestBody.create(content, OCTET_STREAM))
                .build();
        Request request = new Request.Builder()
                .url(HttpAPIRoute.ASSET_UPLOAD.toFullURL())
                .post(requestBody)
                .addHeader("Authorization", client.getNetworkClient().getTokenWithPrefix())
                .build();
        return JsonParser.parseString(client.getNetworkClient().call(request)).getAsJsonObject().getAsJsonObject("data").get("url").getAsString();
    }

    @Override
    public void removeInvite(String urlCode) {
        client.getNetworkClient().post(HttpAPIRoute.INVITE_DELETE.toFullURL(),
                Collections.singletonMap("url_code", urlCode)
        );
    }

    @Override
    public PageIterator<Collection<Game>> getGames() {
        return new GameIterator(client);
    }

    @Override
    public Game createGame(String name, @Nullable String icon) {
        Map<?, ?> body;
        if (icon != null) {
            body = new MapBuilder()
                    .put("name", name)
                    .put("icon", icon)
                    .build();
        } else {
            body = Collections.singletonMap("name", name);
        }
        JsonObject object = client.getNetworkClient().post(HttpAPIRoute.GAME_CREATE.toFullURL(), body);
        Game game = client.getEntityBuilder().buildGame(object);
        client.getStorage().addGame(game);
        return game;
    }

    @Override
    public void setPlaying(@Nullable Game game) {
        if (game != null) {
            Map<String, Object> body = new MapBuilder()
                    .put("data_type", 1)
                    .put("id", game.getId())
                    .build();
            client.getNetworkClient().post(HttpAPIRoute.GAME_CREATE_ACTIVITY.toFullURL(), body);
        } else {
            client.getNetworkClient().post(HttpAPIRoute.GAME_DELETE_ACTIVITY.toFullURL(), Collections.singletonMap("data_type", 1));
        }
    }

    @Override
    public void setListening(@NotNull String softwareName, @NotNull String singerName, @NotNull String musicName) {
        Validate.isTrue(SUPPORTED_MUSIC_SOFTWARES.contains(softwareName), "Unsupported music software name.");
        Map<String, Object> body = new MapBuilder()
                .put("data_type", 2)
                .put("software", softwareName)
                .put("singer", singerName)
                .put("music_name", musicName)
                .build();
        client.getNetworkClient().post(HttpAPIRoute.GAME_CREATE_ACTIVITY.toFullURL(), body);
    }

    @Override
    public void stopListening() {
        client.getNetworkClient().post(HttpAPIRoute.GAME_DELETE_ACTIVITY.toFullURL(), Collections.singletonMap("data_type", 2));
    }
}
