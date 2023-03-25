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

package snw.kookbc.interfaces.network.webhook;

import snw.kookbc.interfaces.Lifecycle;

public interface WebhookServer extends Lifecycle {
    
    // Should only be called once during its lifecycle.
    // So its implementations should be protected.
    // Only for implementation use.
    void setHandler(RequestHandler handler);

    // Set the endpoint of the Webhook handler, should be called BEFORE THE SERVER STARTS.
    void setEndpoint(String path);

}
