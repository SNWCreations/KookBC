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

package snw.kookbc.impl.network.webhook;

import org.jetbrains.annotations.ApiStatus;
import snw.jkook.config.ConfigurationSection;
import snw.kookbc.impl.CoreImpl;
import snw.kookbc.impl.KBCClient;
import snw.kookbc.interfaces.network.webhook.WebhookNetworkSystem;

import java.io.File;

@Deprecated // use KBCClient with WebhookNetworkSystem instead
@ApiStatus.ScheduledForRemoval(inVersion = "0.29.0")
public class WebHookClient extends KBCClient {
    private final WebhookNetworkSystem whNetworkSystem;
    public WebHookClient(CoreImpl core, ConfigurationSection config, File pluginsFolder, String token) {
        super(core, config, pluginsFolder, token, null, null, null, null, null, null, null);
        this.whNetworkSystem = new JLHttpWebhookNetworkSystem(this, null);
    }

    @Override
    protected void startNetwork() {
        whNetworkSystem.start();
    }

    @Override
    protected void shutdownNetwork() {
        whNetworkSystem.stop();
    }
}
