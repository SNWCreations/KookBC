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

package snw.kookbc.impl.plugin;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import snw.jkook.plugin.*;
import snw.jkook.util.Validate;
import snw.kookbc.impl.KBCClient;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Objects;
import java.util.stream.Stream;

import static snw.kookbc.util.Util.getVersionDifference;

public class SimplePluginManager implements PluginManager {
    private final KBCClient client;
    private final Collection<Plugin> plugins = new ArrayList<>();

    public SimplePluginManager(KBCClient client) {
        this.client = client;
    }

    @Override
    public @Nullable Plugin getPlugin(String name) {
        return plugins.stream()
                .filter(IT -> Objects.equals(IT.getDescription().getName(), name))
                .findFirst()
                .orElse(null);
    }

    @Override
    public Plugin[] getPlugins() {
        return plugins.toArray(new Plugin[0]);
    }

    @Override
    public boolean isPluginEnabled(String name) {
        Plugin plugin = getPlugin(name);
        return plugin != null && plugin.isEnabled();
    }

    @Override
    public boolean isPluginEnabled(Plugin plugin) {
        return plugin.isEnabled();
    }

    @Override
    public @NotNull Plugin loadPlugin(File file) throws InvalidPluginException {
        Plugin plugin = new SimplePluginClassLoader(client).loadPlugin(file);
        PluginDescription description = plugin.getDescription();
        int diff = getVersionDifference(description.getApiVersion(), client.getCore().getAPIVersion());
        if (diff == -1) {
            plugin.getLogger().warn("The plugin is using old version of JKook API! We are using {}, got {}", client.getCore().getAPIVersion(), description.getApiVersion());
        }
        if (diff == 1) {
            throw new InvalidPluginException(String.format("The plugin is using unsupported version of JKook API! We are using %s, got %s", client.getCore().getAPIVersion(), description.getApiVersion()));
        }
        return plugin;
    }

    @Override
    public @NotNull Plugin[] loadPlugins(File directory) {
        Collection<Plugin> plugins = new ArrayList<>();
        Validate.isTrue(directory.isDirectory(), "The provided file object is not a directory.");
        File[] files = directory.listFiles(pathname -> pathname.getName().endsWith(".jar"));
        if (files != null) {
            for (File file : files) {
                Plugin plugin;
                try {
                    plugin = loadPlugin(file);
                } catch (Throwable e) {
                    client.getCore().getLogger().error("Unable to load a plugin.", e);
                    continue;
                }
                Stream<Plugin> samePluginStream = plugins.stream().filter(IT -> Objects.equals(IT.getDescription().getName(), plugin.getDescription().getName()));
                if (samePluginStream.findFirst().isPresent()) {
                    client.getCore().getLogger().error(String.format("We have found the same plugin name \"%s\" from two plugin files: %s and %s, both of them won't be returned.", plugin.getDescription().getName(), plugin.getFile(), samePluginStream.findFirst().get().getFile()));
                    plugins.remove(samePluginStream.findFirst().get());
                } else {
                    plugins.add(plugin);
                }
            }
        }
        return plugins.toArray(new Plugin[0]);
    }

    @Override
    public void disablePlugins() {
        for (Plugin plugin : plugins) {
            disablePlugin(plugin);
        }
    }

    @Override
    public void clearPlugins() {
        disablePlugins();
        plugins.clear();
    }

    @Override
    public void enablePlugin(Plugin plugin) {
        if (isPluginEnabled(plugin)) return;
        PluginDescription description = plugin.getDescription();
        plugin.getLogger().info("Enabling {} version {}", description.getName(), description.getVersion());
        try {
            plugin.setEnabled(true);
        } catch (Throwable e) {
            client.getCore().getLogger().error("Exception occurred while we attempting to enable the {} plugin.", plugin.getDescription().getName(), e);
            disablePlugin(plugin); // make sure the plugin is still disabled
        }
        if (!plugin.getDataFolder().exists()) {
            //noinspection ResultOfMethodCallIgnored
            plugin.getDataFolder().mkdir();
        }
    }

    @Override
    public void disablePlugin(Plugin plugin) {
        if (!isPluginEnabled(plugin)) return;
        PluginDescription description = plugin.getDescription();
        plugin.getLogger().info("Disabling {} version {}", description.getName(), description.getVersion());
        try {
            plugin.setEnabled(false);
        } catch (Throwable e) {
            client.getCore().getLogger().error("Exception occurred while we attempting to disable the {} plugin.", plugin.getDescription().getName(), e);
        }
//        if (plugin.getClass().getClassLoader() instanceof SimplePluginClassLoader) {
//            try {
//                ((SimplePluginClassLoader) plugin.getClass().getClassLoader()).close();
//            } catch (IOException e) {
//                client.getCore().getLogger().error("Unexpected IOException while we attempting to close the PluginClassLoader.", e);
//            }
//        }
    }

    @Override
    public void addPlugin(Plugin plugin) {
        plugins.add(plugin);
    }

    @Override
    public void removePlugin(Plugin plugin) {
        plugins.remove(plugin);
    }
}
