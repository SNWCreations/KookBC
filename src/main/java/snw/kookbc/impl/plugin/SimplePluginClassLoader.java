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
import org.yaml.snakeyaml.Yaml;
import snw.jkook.Core;
import snw.jkook.plugin.InvalidPluginException;
import snw.jkook.plugin.Plugin;
import snw.jkook.plugin.PluginClassLoader;
import snw.jkook.plugin.PluginDescription;
import snw.jkook.util.Validate;
import snw.kookbc.impl.KBCClient;
import snw.kookbc.impl.launch.LaunchClassLoader;
import snw.kookbc.util.Util;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;

public class SimplePluginClassLoader extends PluginClassLoader {
    private final KBCClient client;
    private PluginDescription description;
    private final File file;

    private final URLClassLoader parent;

    public SimplePluginClassLoader(KBCClient client, File file, URLClassLoader classLoader) throws MalformedURLException {
        super(new URL[]{file.toURI().toURL()}, classLoader);
        this.file = file;
        this.client = client;
        this.parent = classLoader;
        if (this.parent instanceof LaunchClassLoader) {
            ((LaunchClassLoader) this.parent).addURL(file.toURI().toURL());
        }
    }

    private void initMixins() {
        Map<String, File> map = new HashMap<>();
        try (JarFile jarFile = new JarFile(file)) {
            Enumeration<JarEntry> enumeration = jarFile.entries();
            while (enumeration.hasMoreElements()) {
                JarEntry jarEntry = enumeration.nextElement();
                String name = jarEntry.getName();
                if (name.startsWith("mixin.") && name.endsWith(".json")) {
                    map.put(name, file);
                }
            }
        } catch (IOException e) {
            throw new InvalidPluginException(e);
        }
        if (!map.isEmpty()) {
            if (!Util.isStartByLaunch()) {
                client.getCore().getLogger().warn(
                        "{} v{} plugin is using the Mixin framework. Please use 'Launch' mode to enable support for Mixin",
                        description.getName(),
                        description.getVersion()
                );
                return;
            }
            try {
                for (Map.Entry<String, File> entry : map.entrySet()) {
                    String name = entry.getKey();
                    try (JarFile jarFile = new JarFile(file)) {
                        ZipEntry zipEntry = jarFile.getEntry(name);
                        client.getPluginMixinConfigManager().add(description, name, jarFile.getInputStream(zipEntry));
                    }
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
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
                file,
                new PluginLogger(description.getName(), LoggerFactory.getLogger(cls)),
                client.getCore()
        );
        return plugin;
    }

    @Override
    protected Plugin loadPlugin0(File file) throws Exception {
        Validate.isTrue(file.exists(), "The Plugin file does not exists.");
        Validate.isTrue(file.isFile(), "The Plugin file is invalid.");
        Validate.isTrue(file.canRead(), "The Plugin file does not accessible. (We can't read it!)");

        // load the given file as JarFile
        try (final JarFile jar = new JarFile(file)) { // try-with-resources!
            // try to find plugin.yml
            JarEntry entry = jar.getJarEntry("plugin.yml");
            if (entry == null) {
                throw new IllegalArgumentException("We cannot find plugin.yml ."); // plugin.yml is not found, so we don't know where is the main class
            }
            // or we should read the plugin.yml and parse it to get information
            final InputStream plugin = jar.getInputStream(entry);
            final Yaml parser = new Yaml();

            try {
                final Map<String, Object> ymlContent = parser.load(plugin);
                // noinspection unchecked
                description = new PluginDescription(
                        Objects.requireNonNull(ymlContent.get("name"), "name is missing").toString(),
                        Objects.requireNonNull(ymlContent.get("version"), "version is missing").toString(),
                        Objects.requireNonNull(ymlContent.get("api-version"), "api-version is missing").toString(),
                        ymlContent.getOrDefault("description", "").toString(),
                        ymlContent.getOrDefault("website", "").toString(),
                        Objects.requireNonNull(ymlContent.get("main"), "main is missing").toString(),
                        (List<String>) ymlContent.getOrDefault("authors", Collections.emptyList()),
                        (List<String>) ymlContent.getOrDefault("depend", Collections.emptyList()),
                        (List<String>) ymlContent.getOrDefault("softdepend", Collections.emptyList())
                );
            } catch (ClassCastException e) {
                throw new IllegalArgumentException("Invalid plugin.yml", e);
            }
            initMixins();

            // if the class has already loaded, a conflict has been found.
            // so many things can cause the conflict, such as a class with the same binary name, or the Plugin author trying to use internal classes (e.g. java.lang.Object)
            if (findLoadedClass(description.getMainClassName()) != null) {
                throw new IllegalArgumentException("The main class defined in plugin.yml has already been defined in the VM.");
            }

            return loadPlugin1(file, description);
        }
    }

    @Override
    protected Plugin loadPlugin1(File file, PluginDescription description) throws Exception {
        // No check, because the Exception will be handled by the caller
        Class<? extends Plugin> main = loadClass(description.getMainClassName(), true).asSubclass(Plugin.class);

        if (main.getDeclaredConstructors().length != 1) {
            throw new IllegalAccessException("Unexpected constructor count, expected 1, got " + main.getDeclaredConstructors().length);
        }

        return construct(main, description);
    }

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        try {
            return super.findClass(name);
        } catch (ClassNotFoundException e) {
            // Try to load class from other known plugin
            for (Plugin plugin : client.getCore().getPluginManager().getPlugins()) {
                ClassLoader classLoader = plugin.getClass().getClassLoader();
                if (classLoader == this) {
                    continue;
                }
                try {
                    return classLoader.loadClass(name);
                } catch (ClassNotFoundException ignored) {
                }
            }
        }
        throw new ClassNotFoundException(name);
    }
}
