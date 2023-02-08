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

import org.spongepowered.asm.mixin.Mixins;
import snw.jkook.plugin.PluginDescription;
import snw.kookbc.LaunchMain;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

// The mixin config manager for plugins.
// author: huanmeng_qwq
// added since 2023/1/24
public class PluginMixinConfigManager {
    private final File tempDir;

    public PluginMixinConfigManager() throws IOException {
        this.tempDir = Files.createTempDirectory("KookBC-Mixin").toFile();
        this.tempDir.deleteOnExit();
        LaunchMain.classLoader.addURL(tempDir.toURI().toURL());
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
}
