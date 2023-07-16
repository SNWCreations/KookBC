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

package snw.kookbc.impl.network.ws;

import snw.kookbc.impl.KBCClient;
import snw.kookbc.interfaces.network.ws.WebSocketNetworkSystem;

public class OkhttpWebSocketNetworkSystem implements WebSocketNetworkSystem {
    protected final KBCClient client;
    protected Connector connector;

    public OkhttpWebSocketNetworkSystem(KBCClient client) {
        this.client = client;
    }

    @Override
    public void start() {
        if (this.connector != null) {
            return;
        }
        this.connector = new Connector(client);
        this.connector.start();
    }

    @Override
    public void stop() {
        if (this.connector != null) {
            this.connector.shutdown();
        }
    }

    public Connector getConnector() {
        return connector;
    }

    @Override
    public boolean isConnected() {
        return connector != null && connector.isConnected();
    }
}
