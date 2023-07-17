package snw.kookbc.launcher;

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

    protected Launcher() {
        if (launcher != null) {
            throw new RuntimeException("Launcher has been initialized!");
        }
        launcher = this;
    }

    public ClassLoader getCustomLoader() {
        return this.getClass().getClassLoader();
    }

    public ClassLoader getPluginClassLoader(Class<?> caller){
        return caller.getClassLoader();
    }

    public ClassLoader getContextClassLoader() {
        return Thread.currentThread().getContextClassLoader();
    }

}
