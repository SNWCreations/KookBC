package snw.kookbc.interfaces;

public interface LazyLoadable {
    boolean isCompleted();

    void initialize();

    default void lazyload() {
        if (!isCompleted()) {
            initialize();
        }
    }
}
