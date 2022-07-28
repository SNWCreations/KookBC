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

package snw.kookbc.impl.network.webhook;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import snw.kookbc.impl.KBCClient;
import snw.kookbc.impl.network.Listener;
import snw.kookbc.impl.network.ListenerImpl;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import static snw.kookbc.impl.network.MessageProcessor.decompressDeflate;

public class SimpleHttpHandler implements HttpHandler {
    protected final KBCClient client;
    protected final Listener listener;

    public SimpleHttpHandler(KBCClient client) {
        this.client = client;
        listener = new ListenerImpl(client.getConnector());
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String res;
        byte[] bytes = inputStreamToByteArray(exchange.getRequestBody());
        if (client.getConfig().getBoolean("compress")) {
            res = new String(decompressDeflate(bytes));
        } else {
            res = new String(bytes);
        }
        listener.parseEvent(res);
        exchange.sendResponseHeaders(200, -1);
        exchange.getResponseBody().close();
    }

    protected byte[] inputStreamToByteArray(InputStream stream) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();

        int nRead;
        byte[] data = new byte[16384];

        while ((nRead = stream.read(data, 0, data.length)) != -1) {
            buffer.write(data, 0, nRead);
        }

        return buffer.toByteArray();
    }
}
