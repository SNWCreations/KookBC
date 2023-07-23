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

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import snw.jkook.plugin.*;
import snw.jkook.util.Validate;
import snw.kookbc.impl.KBCClient;
import snw.kookbc.impl.command.CommandManagerImpl;
import snw.kookbc.impl.launch.AccessClassLoader;
import snw.kookbc.launcher.Launcher;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.jar.JarFile;

import static snw.kookbc.util.Util.closeLoaderIfPossible;
import static snw.kookbc.util.Util.getVersionDifference;

public class SimplePluginManager implements PluginManager {
    private KBCClient client;
    private Logger logger;
    private final Collection<Plugin> plugins = new ArrayList<>();
    private final Map<Predicate<File>, Function<ClassLoader, PluginLoader>> loaderMap = new LinkedHashMap<>();

    public SimplePluginManager(KBCClient client) {
        this(client, client.getCore().getLogger());
    }

    public SimplePluginManager(KBCClient client, Logger logger) {
        this.client = client;
        this.logger = logger;
        this.registerPluginLoader(f -> {
            if (!f.getName().endsWith(".jar"))
                return false;
            try (JarFile jarFile = new JarFile(f)) {
                return jarFile.getJarEntry("plugin.yml") != null;
            } catch (IOException e) {
                return false;
            }
        }, this::createPluginLoader); // ensure overrides will apply
    }

    public void setClient(KBCClient client) {
        if (this.client != null) {
            throw new UnsupportedOperationException("Client already set");
        }
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
        return loadPlugin0(file, true);
    }

    protected Plugin loadPlugin0(File file, boolean failIfNoLoader) throws InvalidPluginException {
        // We won't close the ClassLoader, because Plugin#getResource need the ClassLoader to keep open.
        // Otherwise, Plugin#getResource will not work correctly.
        // If you want to reload a plugin, or fully uninstall a plugin, close the ClassLoader manually.
        // An example is following:
        // pluginManager.disablePlugin(plugin);
        // ((URLClassLoader) plugin.getClass().getClassLoader()).close();
        Plugin plugin;
        PluginLoader loader;
        ClassLoader parent = Launcher.instance().getPluginClassLoader(getClass());
        if (!(parent instanceof MarkedClassLoader)) {
            parent = null;
        }
        loader = createPluginLoaderForFile(file, parent);
        if (loader == null) {
            if (failIfNoLoader) {
                throw new InvalidPluginException("There is no loader can load the file " + file);
            }
            return null;
        }
        try {
            plugin = loader.loadPlugin(file);
        } catch (InvalidPluginException e) {
            closeLoaderIfPossible(loader);
            throw e; // loader created, but plugin not valid
        }
        PluginDescription description = plugin.getDescription();
        int diff = getVersionDifference(description.getApiVersion(), client.getCore().getAPIVersion());
        if (diff == -1) {
            plugin.getLogger().warn("The plugin is using old version of JKook API! We are using {}, got {}", client.getCore().getAPIVersion(), description.getApiVersion());
        }
        if (diff == 1) {
            closeLoaderIfPossible(loader); // plugin won't be returned, so the loader should be closed to prevent resource leak
            throw new InvalidPluginException(String.format("The plugin is using unsupported version of JKook API! We are using %s, got %s", client.getCore().getAPIVersion(), description.getApiVersion()));
        }
        return plugin;
    }

    @Override
    public @NotNull Plugin[] loadPlugins(File directory) {
        Validate.isTrue(directory.isDirectory(), "The provided file object is not a directory.");
        File[] files = directory.listFiles(File::isFile);
        if (files != null) {
            Collection<Plugin> plugins = new ArrayList<>(files.length);
            for (File file : files) {
                Plugin plugin;
                try {
                    plugin = loadPlugin0(file, false);
                } catch (Throwable e) {
                    logger.error("Unable to load a plugin in the provided file {}", file, e);
                    continue;
                }
                if (plugin == null) {
                    continue; // no suitable loader can be created, not a valid plugin file.
                }
                boolean shouldAdd = true;
                for (final Plugin p : plugins) {
                    if (Objects.equals(p.getDescription().getName(), plugin.getDescription().getName())) {
                        logger.error(
                                "We have found the same plugin name \"{}\" from two plugin files:" +
                                        " {} and {}, the plugin inside {} won't be returned.",
                                plugin.getDescription().getName(),
                                plugin.getFile(),
                                p.getFile(),
                                plugin.getFile()
                        );
                        shouldAdd = false;
                    }
                }
                if (shouldAdd) {
                    plugins.add(plugin);
                }
            }
            return plugins.toArray(new Plugin[0]);
        }
        return new Plugin[0];
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
    public void enablePlugin(Plugin plugin) throws UnknownDependencyException {
        if (isPluginEnabled(plugin)) return;
        PluginDescription description = plugin.getDescription();
        plugin.getLogger().info("Enabling {} version {}", description.getName(), description.getVersion());
        if (!plugin.getDataFolder().exists()) {
            //noinspection ResultOfMethodCallIgnored
            plugin.getDataFolder().mkdir();
        }
        for (String dep : description.getDepend()) {
            if (getPlugin(dep) == null) {
                throw new UnknownDependencyException(String.format("Detected unknown dependency '%s' from plugin '%s'", dep, description.getName()));
            }
        }
        try {
            plugin.setEnabled(true);
        } catch (Throwable e) {
            plugin.getLogger().error("Unable to enable this plugin", e);
            disablePlugin(plugin); // make sure the plugin is still disabled
        }
    }

    @Override
    public void disablePlugin(Plugin plugin) {
        if (!isPluginEnabled(plugin)) return;
        PluginDescription description = plugin.getDescription();
        plugin.getLogger().info("Disabling {} version {}", description.getName(), description.getVersion());
        // cancel tasks
        client.getCore().getScheduler().cancelTasks(plugin);
        client.getCore().getEventManager().unregisterAllHandlers(plugin);
        // unregister commands
        try {
            ((CommandManagerImpl) client.getCore().getCommandManager()).getCommandMap().unregisterAll(plugin);
            plugin.setEnabled(false);
        } catch (Throwable e) {
            plugin.getLogger().error("Exception occurred when we are disabling this plugin", e);
        }
//        if (plugin.getClass().getClassLoader() instanceof SimplePluginClassLoader) {
//            try {
//                ((SimplePluginClassLoader) plugin.getClass().getClassLoader()).close();
//            } catch (IOException e) {
//                logger.error("Unexpected IOException while we're attempting to close the PluginClassLoader.", e);
//            }
//        }
    }

    @Override
    public void addPlugin(Plugin plugin) {
        if (plugins.stream().anyMatch(IT -> Objects.equals(IT.getDescription().getName(), plugin.getDescription().getName()))) {
            throw new IllegalArgumentException("The provided plugin name is already in use.");
        }
        plugins.add(plugin);
    }

    @Override
    public void removePlugin(Plugin plugin) {
        plugins.remove(plugin);
    }

    @Override
    public void registerPluginLoader(Predicate<File> predicate, Function<ClassLoader, PluginLoader> provider) {
        Validate.notNull(predicate, "Predicate cannot be null");
        Validate.notNull(provider, "Provider cannot be null");
        loaderMap.put(predicate, provider);
    }

    protected PluginLoader createPluginLoader(@Nullable ClassLoader parent) {
        return new SimplePluginClassLoader(client, AccessClassLoader.of(parent));
    }

    protected @Nullable PluginLoader createPluginLoaderForFile(File file, @Nullable ClassLoader parent) {
        for (Map.Entry<Predicate<File>, Function<ClassLoader, PluginLoader>> entry : loaderMap.entrySet()) {
            final Predicate<File> condition = entry.getKey();
            if (condition.test(file)) {
                final Function<ClassLoader, PluginLoader> provider = entry.getValue();
                return provider.apply(parent);
            }
        }
        return null;
    }

    public Map<Predicate<File>, Function<ClassLoader, PluginLoader>> getLoaderProviders() {
        return Collections.unmodifiableMap(loaderMap);
    }
}
