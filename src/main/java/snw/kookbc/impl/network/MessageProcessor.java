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

package snw.kookbc.impl.network;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import okio.ByteString;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import snw.kookbc.impl.KBCClient;

import java.io.IOException;
import java.net.ProtocolException;
import java.util.zip.DataFormatException;

import static snw.kookbc.util.GsonUtil.get;
import static snw.kookbc.util.GsonUtil.has;
import static snw.kookbc.util.Util.decompressDeflate;

public class MessageProcessor extends WebSocketListener {
    private final KBCClient client;
    private final Connector connector;
    private final Listener listener;

    public MessageProcessor(KBCClient client) {
        this.client = client;
        this.connector = client.getConnector();
        listener = ListenerFactory.getListener(client);
    }

    @Override
    public void onOpen(@NotNull WebSocket webSocket, @NotNull Response response) {
        super.onOpen(webSocket, response);
        Thread.currentThread().setName("Network Thread");
    }

    // for non-compressed messages
    @Override
    public void onMessage(@NotNull WebSocket webSocket, @NotNull String text) {
        super.onMessage(webSocket, text);
        JsonObject object = JsonParser.parseString(text).getAsJsonObject();
        Frame frame = new Frame(get(object, "s").getAsInt(), has(object, "sn") ? get(object, "sn").getAsInt() : -1, object.getAsJsonObject("d"));
        listener.executeEvent(frame);
    }

    // for compressed messages, so we will extract it before processing
    @Override
    public void onMessage(@NotNull WebSocket webSocket, @NotNull ByteString bytes) {
        super.onMessage(webSocket, bytes);
        String res;
        try {
            res = new String(decompressDeflate(bytes.toByteArray()));
        } catch (DataFormatException | IOException e) {
            client.getCore().getLogger().error("Unable to decompress data", e);
            return;
        }
        JsonObject object = JsonParser.parseString(res).getAsJsonObject();
        Frame frame = new Frame(get(object, "s").getAsInt(), has(object, "sn") ? get(object, "sn").getAsInt() : -1, object.getAsJsonObject("d"));
        listener.executeEvent(frame);
    }

    @Override
    public void onFailure(@NotNull WebSocket webSocket, @NotNull Throwable t, @Nullable Response response) {
        super.onFailure(webSocket, t, response);
        if (!(t instanceof ProtocolException)) {
            connector.getParent().getCore().getLogger().error("Unexpected failure occurred in the Network module. We will restart the Network module.");
            connector.getParent().getCore().getLogger().error("Response is following: {}", response);
            connector.getParent().getCore().getLogger().error("Stacktrace is following.", t);
        }
        webSocket.close(1000, "User Closed Service");
        connector.requestReconnect();
    }

}
