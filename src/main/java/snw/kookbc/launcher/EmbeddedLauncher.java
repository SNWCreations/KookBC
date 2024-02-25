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
package snw.kookbc.launcher;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import snw.jkook.JKook;
import snw.jkook.config.file.YamlConfiguration;
import snw.kookbc.impl.CoreImpl;
import snw.kookbc.impl.KBCClient;
import snw.kookbc.impl.command.CommandManagerImpl;

import java.io.File;

@SuppressWarnings("unused")
public class EmbeddedLauncher extends Launcher {
    protected String token;
    protected final YamlConfiguration config;
    protected final Logger logger;
    protected final File pluginsFolder;

    protected KBCClient client;
    protected CoreImpl core;

    public EmbeddedLauncher(@NotNull File configFile) {
        this(YamlConfiguration.loadConfiguration(configFile), null, null, null);
    }

    public EmbeddedLauncher(@NotNull YamlConfiguration config, @Nullable String token, @Nullable Logger logger, @Nullable File pluginsFolder) {
        super();
        this.config = config;
        this.token = token;
        this.logger = logger;
        this.pluginsFolder = pluginsFolder;
    }

    public KBCClient getClient() {
        if (client != null) {
            return client;
        }
        build();
        return client;
    }

    public CoreImpl getCore() {
        if (core != null) {
            return core;
        }
        build();
        return core;
    }

    protected void build() {
        if (logger != null) {
            core = new CoreImpl(logger);
        } else {
            core = new CoreImpl();
        }
        JKook.setCore(core);

        if (token == null || token.isEmpty()) {
            String configToken = config.getString("token");
            if (configToken != null && !configToken.isEmpty()) {
                token = configToken;
            } else {
                throw new IllegalArgumentException("Token is not set");
            }
        }
        client = new KBCClient(core, config, pluginsFolder, token, CommandManagerImpl::new,
                null, null, null, null, null, null);
    }
}
