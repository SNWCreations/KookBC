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
import org.spongepowered.asm.mixin.Mixins;
import snw.jkook.plugin.InvalidPluginException;
import snw.jkook.plugin.PluginDescription;
import snw.kookbc.impl.launch.AccessClassLoader;
import snw.kookbc.util.Util;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;

// The mixin config manager for plugins.
// author: huanmeng_qwq
// added since 2023/1/24
public class MixinPluginManager {
    private final File tempDir;
    private static Logger logger = LoggerFactory.getLogger("MixinPlugin");
    private static final MixinPluginManager INSTANCE;
    private AccessClassLoader cacheClassloader;

    static {
        try {
            INSTANCE = new MixinPluginManager();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static MixinPluginManager instance() {
        return INSTANCE;
    }

    private MixinPluginManager() throws IOException {
        this.tempDir = Files.createTempDirectory("KookBC-Mixin").toFile();
        this.tempDir.deleteOnExit();
    }

    protected void setCacheClassloader(AccessClassLoader classloader) {
        if (this.cacheClassloader != null) {
            return;
        }
        this.cacheClassloader = classloader;
        try {
            this.cacheClassloader.addURL(tempDir.toURI().toURL());
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
    }

    public void add(PluginDescription description, String name, InputStream stream) throws IOException {
        String configName = description.getName() + "-" + name;
        addConfig(stream, configName);
    }

    public void addConfig(InputStream stream, String configName) throws IOException {
        String targetName = configName;
        Path path = new File(tempDir, configName).toPath();
        int duplicate = 0;
        while (path.toFile().exists()) {
            ++duplicate;
            configName = targetName + "_" + duplicate;
            path = new File(tempDir, configName).toPath();
        }
        try (InputStream inputStream = stream) {
            Files.copy(inputStream, path);
            Mixins.addConfiguration(path.toFile().getName());
        }
    }

    public void loadFolder(AccessClassLoader classLoader, File folder) {
        if (!folder.exists()) {
            return;
        }
        if (folder.isDirectory()) {
            File[] files = folder.listFiles();
            if (files != null) {
                for (File file : files) {
                    loadJarPlugin(classLoader, file);
                }
            }
        } else {
            // maybe file?
            loadJarPlugin(classLoader, folder);
        }
    }

    public void loadJarPlugin(AccessClassLoader classLoader, File file) {
        if (!file.exists()) {
            return;
        }
        if (!file.getName().endsWith(".jar")) {
            return;
        }
        setCacheClassloader(classLoader);
        Set<String> confNameSet = new HashSet<>();
        PluginDescription description;
        try (JarFile jarFile = new JarFile(file)) {
            Enumeration<JarEntry> enumeration = jarFile.entries();
            while (enumeration.hasMoreElements()) {
                JarEntry jarEntry = enumeration.nextElement();
                String name = jarEntry.getName();
                if (name.startsWith("mixin.") && name.endsWith(".json")) {
                    confNameSet.add(name);
                }
            }
            JarEntry entry = jarFile.getJarEntry("plugin.yml");
            if (entry == null) {
                throw new IllegalArgumentException("We cannot find plugin.yml");
            }

            InputStream pluginYmlStream = jarFile.getInputStream(entry);
            description = Util.createDescription(pluginYmlStream);

        } catch (IOException e) {
            throw new InvalidPluginException(e);
        }
        if (!confNameSet.isEmpty()) {
            if (!Util.isStartByLaunch()) {
                logger.warn(
                        "[{}] {} v{} plugin is using the Mixin framework. Please use 'Launch' mode to enable support for Mixin",
                        description.getName(),
                        description.getName(),
                        description.getVersion()
                );
                return;
            }
            try {
                classLoader.addURL(file.toURI().toURL());
                for (String name : confNameSet) {
                    try (JarFile jarFile = new JarFile(file)) {
                        ZipEntry zipEntry = jarFile.getEntry(name);
                        add(description, name, jarFile.getInputStream(zipEntry));
                    }
                }
            } catch (IOException e) {
                throw new InvalidPluginException(e);
            }
        }
    }
}
