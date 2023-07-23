package snw.kookbc.launcher;

import snw.kookbc.impl.launch.AccessClassLoader;
import snw.kookbc.impl.plugin.MixinPluginManager;

import java.io.File;

/**
 * 2023/7/17<br>
 * KookBC<br>
 *
 * @author huanmeng_qwq
 */
public abstract class Launcher {
    private static Launcher launcher;

    public static Launcher instance() {
        return launcher;
    }

    public void onSetup() {
        if (instance() != this) {
            return;
        }
        MixinPluginManager.instance().loadFolder(AccessClassLoader.of(getCustomLoader()), new File("plugins"));
    }

    protected Launcher() {
        if (launcher != null) {
            return;
        }
        launcher = this;
    }

    public ClassLoader getCustomLoader() {
        return this.getClass().getClassLoader();
    }

    public ClassLoader getPluginClassLoader(Class<?> caller) {
        return caller.getClassLoader();
    }

    public ClassLoader getContextClassLoader() {
        return Thread.currentThread().getContextClassLoader();
    }

}
