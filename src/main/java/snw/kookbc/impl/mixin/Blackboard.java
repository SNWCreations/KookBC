package snw.kookbc.impl.mixin;

import org.spongepowered.asm.service.IGlobalPropertyService;
import org.spongepowered.asm.service.IPropertyKey;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static snw.kookbc.impl.launch.Launch.blackboard;

@SuppressWarnings("unchecked")
public class Blackboard implements IGlobalPropertyService {
//    public static Map<String, Object> blackboard = new ConcurrentHashMap<>();

    public Blackboard() {

    }

    public IPropertyKey resolveKey(String name) {
        return new Key(name);
    }

    public final <T> T getProperty(IPropertyKey key) {
        return (T) blackboard.get(key.toString());
    }

    public final void setProperty(IPropertyKey key, Object value) {
        blackboard.put(key.toString(), value);
    }

    public final <T> T getProperty(IPropertyKey key, T defaultValue) {
        return (T) blackboard.getOrDefault(key.toString(), defaultValue);
    }

    public final String getPropertyString(IPropertyKey key, String defaultValue) {
        Object value = blackboard.get(key.toString());
        return value != null ? value.toString() : defaultValue;
    }

    static class Key implements IPropertyKey {
        private final String key;

        Key(String key) {
            this.key = key;
        }

        public String toString() {
            return this.key;
        }
    }
}
