package snw.kookbc.impl.plugin;

import org.spongepowered.asm.mixin.Mixins;
import snw.jkook.plugin.PluginDescription;
import snw.kookbc.impl.launch.Launch;
import snw.kookbc.util.Util;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;

/**
 * 2023/1/24<br>
 * KookBC<br>
 *
 * @author huanmeng_qwq
 */
public class PluginMixinConfigManager {
    private final File tempDir;

    public PluginMixinConfigManager() throws IOException {
        this.tempDir = Files.createTempDirectory("KookBC-Mixin").toFile();
        this.tempDir.deleteOnExit();
        Launch.classLoader.addURL(tempDir.toURI().toURL());
    }

    public void add(PluginDescription description, String name, InputStream stream) {
        String fullName = description.getName() + "-" + name;
        File target = new File(tempDir, fullName);
        try (InputStream inputStream = stream) {
            Files.copy(inputStream, target.toPath());
            Mixins.addConfiguration(fullName);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
