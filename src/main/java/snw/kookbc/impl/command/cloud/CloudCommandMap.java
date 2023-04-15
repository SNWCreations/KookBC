package snw.kookbc.impl.command.cloud;

import snw.jkook.command.JKookCommand;
import snw.jkook.plugin.Plugin;
import snw.kookbc.impl.command.CommandManagerImpl;
import snw.kookbc.impl.command.SimpleCommandMap;

/**
 * @author huanmeng_qwq
 */
public class CloudCommandMap extends SimpleCommandMap {
    protected final CloudCommandManagerImpl cloudCommandManager;
    protected CommandManagerImpl commandManager;

    public CloudCommandMap(CloudCommandManagerImpl cloudCommandManager) {
        this.cloudCommandManager = cloudCommandManager;
    }

    public void initialize(CommandManagerImpl commandManager) {
        if (this.commandManager != null) {
            return;
        }
        this.commandManager = commandManager;
    }

    public void register(Plugin plugin, JKookCommand command) {
        super.register(plugin, command);
        this.cloudCommandManager.registerJKook(command, plugin, this.commandManager);
    }

    public void unregister(JKookCommand command) {
        super.unregister(command);
        cloudCommandManager.unregisterJKookCommand(command);
    }

    public void unregisterAll(Plugin plugin) {
        super.unregisterAll(plugin);
        cloudCommandManager.unregisterJKookCommands(plugin);
    }

    public CloudCommandManagerImpl cloudCommandManager() {
        return cloudCommandManager;
    }
}
