package snw.kookbc.interfaces;

public interface LazyLoadable {
    boolean isCompleted();

    void initialize();

    default void initIfNeeded() {
        if (!isCompleted()) {
            initialize();
        }
    }
}
