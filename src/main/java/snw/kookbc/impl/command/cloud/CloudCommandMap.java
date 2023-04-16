package snw.kookbc.impl.command.cloud;

import snw.jkook.command.JKookCommand;
import snw.jkook.plugin.Plugin;
import snw.kookbc.impl.command.SimpleCommandMap;

/**
 * @author huanmeng_qwq
 */
public class CloudCommandMap extends SimpleCommandMap {
    protected CloudCommandManagerImpl parent;

    public void init(CloudCommandManagerImpl parent) {
        if (this.parent != null) {
            throw new IllegalStateException("Already initialized");
        }
        this.parent = parent;
    }

    public void register(Plugin plugin, JKookCommand command) {
        super.register(plugin, command);
        this.parent.getCloudCommandManager().registerJKookCommand(plugin, command);
    }

    public void unregister(JKookCommand command) {
        super.unregister(command);
        Plugin plugin = getOwnerOfCommand(command);
        this.parent.getCloudCommandManager().unregisterJKookCommand(command);
    }

    public void unregisterAll(Plugin plugin) {
        super.unregisterAll(plugin);
        this.parent.getCloudCommandManager().unregisterAll(plugin);
    }

    protected Plugin getOwnerOfCommand(JKookCommand command) {
        return getView(false).get(command.getRootName()).getPlugin();
    }
}
