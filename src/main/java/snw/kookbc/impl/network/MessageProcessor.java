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

import java.io.ByteArrayOutputStream;
import java.net.ProtocolException;
import java.util.zip.Inflater;

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
        connector.getParent().getCore().getLogger().debug("MessageProcessor#onMessage(String) got call. Response: {}", text);
        JsonObject object = JsonParser.parseString(text).getAsJsonObject();
        Frame frame = new Frame(object.get("s").getAsInt(), object.get("sn").getAsInt(), object.getAsJsonObject("d"));
        listener.executeEvent(frame);
    }

    // for compressed messages, so we will extract it before processing
    @Override
    public void onMessage(@NotNull WebSocket webSocket, @NotNull ByteString bytes) {
        super.onMessage(webSocket, bytes);
        String res;
        try {
            res = new String(decompressDeflate(bytes.toByteArray()));
        } catch (RuntimeException e) {
            client.getCore().getLogger().error("Unable to decompress data", e);
            return;
        }
        connector.getParent().getCore().getLogger().debug("MessageProcessor#onMessage(ByteString) got call. Response: {}", res);
        JsonObject object = JsonParser.parseString(res).getAsJsonObject();
        Frame frame = new Frame(object.get("s").getAsInt(), object.get("sn").getAsInt(), object.getAsJsonObject("d"));
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

    public static byte[] decompressDeflate(byte[] data) {
        Inflater decompressor = new Inflater();
        decompressor.reset();
        decompressor.setInput(data);

        try (ByteArrayOutputStream o = new ByteArrayOutputStream(data.length)) {
            byte[] buf = new byte[1024];
            while (!decompressor.finished()) {
                int i = decompressor.inflate(buf);
                o.write(buf, 0, i);
            }
            return o.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Unexpected exception happened while we attempting to decompress the ZLIB/DEFLATE compressed data.", e);
        } finally {
            decompressor.end();
        }
    }

}
