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

import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import okio.ByteString;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import snw.jkook.JKook;

import java.io.ByteArrayOutputStream;
import java.net.ProtocolException;
import java.util.zip.Inflater;

public class MessageProcessor extends WebSocketListener {
    private final Connector connector;
    private final Listener listener;

    public MessageProcessor(Connector connector) {
        this.connector = connector;
        listener = new ListenerImpl(connector);
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
        JKook.getLogger().debug("MessageProcessor#onMessage(String) got call. Response: {}", text);
        listener.parseEvent(text);
    }

    // for compressed messages, so we will extract it before processing
    @Override
    public void onMessage(@NotNull WebSocket webSocket, @NotNull ByteString bytes) {
        super.onMessage(webSocket, bytes);
        String res = new String(decompressDeflate(bytes.toByteArray()));
        JKook.getLogger().debug("MessageProcessor#onMessage(ByteString) got call. Response: {}", res);
        listener.parseEvent(res);
    }

    @Override
    public void onClosed(@NotNull WebSocket webSocket, int code, @NotNull String reason) {
        super.onClosed(webSocket, code, reason);
        if (code == 1002) {
            JKook.getLogger().error("Unexpected close response from WebSocket server. We will restart network.");
            connector.setRequireReconnect(true);
        }
    }

    @Override
    public void onFailure(@NotNull WebSocket webSocket, @NotNull Throwable t, @Nullable Response response) {
        super.onFailure(webSocket, t, response);
        if (!(t instanceof ProtocolException)) {
            JKook.getLogger().error("Unexpected failure occurred in the Network module. We will restart the Network module.");
            JKook.getLogger().error("Response is following: {}", response);
            JKook.getLogger().error("Stacktrace is following.", t);
        }
        connector.setRequireReconnect(true);
    }

    public static byte[] decompressDeflate(byte[] data) {
        byte[] output = null;

        Inflater decompressor = new Inflater();
        decompressor.reset();
        decompressor.setInput(data);

        try (ByteArrayOutputStream o = new ByteArrayOutputStream(data.length)) {
            byte[] buf = new byte[1024];
            while (!decompressor.finished()) {
                int i = decompressor.inflate(buf);
                o.write(buf, 0, i);
            }
            output = o.toByteArray();
        } catch (Exception e) {
            JKook.getLogger().error("Unexpected exception happened while we attempting to decompress the ZLIB/DEFLATE compressed data.", e);
        }

        decompressor.end();
        return output;
    }

}
