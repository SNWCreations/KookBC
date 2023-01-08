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

package snw.kookbc.impl.plugin;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import snw.jkook.Core;
import snw.jkook.plugin.Plugin;
import snw.jkook.plugin.PluginClassLoader;
import snw.jkook.plugin.PluginDescription;
import snw.kookbc.impl.KBCClient;

import java.io.File;
import java.lang.reflect.Method;

public class SimplePluginClassLoader extends PluginClassLoader {
    private final KBCClient client;

    public SimplePluginClassLoader(KBCClient client) {
        this.client = client;
    }

    @Override
    protected <T extends Plugin> T construct(final Class<T> cls, final PluginDescription description) throws Exception {
        File dataFolder = new File(client.getPluginsFolder(), description.getName());
        T plugin = cls.getDeclaredConstructor().newInstance();
        Method initMethod = cls.getMethod(
                "init",
                File.class, File.class, PluginDescription.class, File.class, Logger.class, Core.class
        );
        initMethod.invoke(plugin,
                new File(dataFolder, "config.yml"),
                dataFolder,
                description,
                new File(cls.getProtectionDomain().getCodeSource().getLocation().toURI()),
                new PluginLogger(description.getName(), LoggerFactory.getLogger(cls)),
                client.getCore()
        );
        return plugin;
    }

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        try {
            return super.findClass(name);
        } catch (ClassNotFoundException e) {
            // Try to load class from other known plugin
            for (Plugin plugin : client.getCore().getPluginManager().getPlugins()) {
                try {
                    return plugin.getClass().getClassLoader().loadClass(name);
                } catch (ClassNotFoundException ignored) {
                }
            }
        }
        throw new ClassNotFoundException(name);
    }
}
