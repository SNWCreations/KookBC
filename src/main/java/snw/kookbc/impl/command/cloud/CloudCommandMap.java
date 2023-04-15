package snw.kookbc.impl.command.cloud;

import snw.jkook.command.JKookCommand;
import snw.jkook.plugin.Plugin;
import snw.kookbc.impl.command.CommandManagerImpl;
import snw.kookbc.impl.command.SimpleCommandMap;

/**
 * @author huanmeng_qwq
 */
public class CloudCommandMap extends SimpleCommandMap {
    protected final CloudCommandManagerImpl parent;
    protected CommandManagerImpl commandManager;

    public CloudCommandMap(CloudCommandManagerImpl parent) {
        this.parent = parent;
    }

    public void initialize(CommandManagerImpl commandManager) {
        if (this.commandManager != null) {
            return;
        }
        this.commandManager = commandManager;
    }

    public void register(Plugin plugin, JKookCommand command) {
        super.register(plugin, command);
        this.parent.getCloudCommandManager(plugin).registerJKook(command, plugin);
    }

    public void unregister(JKookCommand command) {
        super.unregister(command);
        Plugin plugin = getOwnerOfCommand(command);
        this.parent.getCloudCommandManager(plugin).unregisterJKookCommand(command);
    }

    public void unregisterAll(Plugin plugin) {
        super.unregisterAll(plugin);
        this.parent.getCloudCommandManager(plugin).unregisterJKookCommands(plugin);
    }

    protected Plugin getOwnerOfCommand(JKookCommand command) {
        return getView(false).get(command.getRootName()).getPlugin();
    }
}
