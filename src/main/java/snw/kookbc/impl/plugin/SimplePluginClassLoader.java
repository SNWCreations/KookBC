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
import snw.jkook.plugin.InvalidPluginException;
import snw.jkook.plugin.Plugin;
import snw.jkook.plugin.PluginClassLoader;
import snw.jkook.plugin.PluginDescription;
import snw.kookbc.impl.KBCClient;
import snw.kookbc.util.Util;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;

// The Plugin ClassLoader.
// Call close method on unused instances to ensure the instance will be fully destroyed.
public class SimplePluginClassLoader extends PluginClassLoader {
    public static final Collection<SimplePluginClassLoader> INSTANCES = Collections.newSetFromMap(new WeakHashMap<>());
    private final Map<String, Class<?>> cache = new ConcurrentHashMap<>();
    private final KBCClient client;
    private PluginDescription description;

    public SimplePluginClassLoader(KBCClient client, ClassLoader parent) {
        super(new URL[]{}, parent);
        this.client = client;
        INSTANCES.add(this);
    }

    private void initMixins(File file) {
        Set<String> confNameSet = new HashSet<>();
        try (JarFile jarFile = new JarFile(file)) {
            Enumeration<JarEntry> enumeration = jarFile.entries();
            while (enumeration.hasMoreElements()) {
                JarEntry jarEntry = enumeration.nextElement();
                String name = jarEntry.getName();
                if (name.startsWith("mixin.") && name.endsWith(".json")) {
                    confNameSet.add(name);
                }
            }
        } catch (IOException e) {
            throw new InvalidPluginException(e);
        }
        if (!confNameSet.isEmpty()) {
            if (!Util.isStartByLaunch()) {
                client.getCore().getLogger().warn(
                        "[{}] {} v{} plugin is using the Mixin framework. Please use 'Launch' mode to enable support for Mixin",
                        description.getName(),
                        description.getName(),
                        description.getVersion()
                );
                return;
            }
            try {
                for (String name : confNameSet) {
                    try (JarFile jarFile = new JarFile(file)) {
                        ZipEntry zipEntry = jarFile.getEntry(name);
                        client.getPluginMixinConfigManager().add(description, name, jarFile.getInputStream(zipEntry));
                    }
                }
            } catch (IOException e) {
                throw new InvalidPluginException(e);
            }
        }
    }

    @Override
    protected <T extends Plugin> T construct(final Class<T> cls, final PluginDescription description) throws Exception {
        T plugin = cls.getDeclaredConstructor().newInstance();
        Method initMethod = cls.getMethod(
                "init",
                File.class, File.class, PluginDescription.class, File.class, Logger.class, Core.class
        );
        File pluginFile;
        final URL location = cls.getProtectionDomain().getCodeSource().getLocation();
        if (location.getFile().endsWith(".class")) {
            if (!location.getFile().contains("!/")) {
                throw new IllegalArgumentException("Cannot obtain the source jar of the main class, location: " + location + ", maybe it is a single class file?");
            }
            String url = location.toString();
            url = url.substring(0, url.indexOf("!/"));
            pluginFile = new File(new URL(url).toURI());
        } else {
            pluginFile = new File(location.toURI());
        }
        File dataFolder = new File(client.getPluginsFolder(), description.getName());
        initMethod.invoke(plugin,
                new File(dataFolder, "config.yml"),
                dataFolder,
                description,
                pluginFile,
                new PrefixLogger(description.getName(), LoggerFactory.getLogger(cls)),
                client.getCore()
        );
        return plugin;
    }

    @Override
    protected Class<? extends Plugin> lookForMainClass(String mainClassName, File file) throws Exception {
        initMixins(file);
        return super.lookForMainClass(mainClassName, file);
    }

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        return findClass0(name, false);
    }

    public final Class<?> findClass0(String name, boolean dontCallOther) throws ClassNotFoundException {
        if (cache.containsKey(name)) {
            return cache.get(name);
        }
        Class<?> result = null;
        try {
            result = super.findClass(name);
        } catch (ClassNotFoundException ignored) {
        }

        // Try to load class from other known instances if needed
        if (!dontCallOther) {
            result = loadFromOther(name);
        }
        if (result == null) {
            throw new ClassNotFoundException(name);
        }
        cache.put(name, result);
        return result;
    }

    protected Class<?> loadFromOther(String name) throws ClassNotFoundException {
        for (SimplePluginClassLoader classLoader : INSTANCES) {
            if (classLoader == null) {
                // Suggested by ChatGPT:
                // The keys in a WeakHashMap are held through weak references,
                // which may be garbage collected when no strong references to them exist.
                // If null checks are not performed while traversing the key set,
                // it may lead to encountering null keys that have already been garbage collected,
                // resulting in a NullPointerException.
                // Therefore, when traversing the key set of a WeakHashMap,
                // it is necessary to perform a null check first and process only non-null keys.
                continue;
            }
            if (classLoader == this) {
                continue;
            }
            try {
                return classLoader.findClass0(name, true); // use true to prevent stack over flow
            } catch (ClassNotFoundException ignored) {
            }
        }
        throw new ClassNotFoundException(name);
    }

    @Override
    public void close() throws IOException {
        INSTANCES.remove(this);
        super.close();
    }
    
}
