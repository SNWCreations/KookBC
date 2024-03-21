package snw.kookbc.launcher;

public abstract class Launcher {
    private static Launcher launcher;

    public static Launcher instance() {
        return launcher;
    }

    public final void onSetup() {
        if (instance() != this) {
            return;
        }
        doSetup();
    }

    protected void doSetup() {
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
