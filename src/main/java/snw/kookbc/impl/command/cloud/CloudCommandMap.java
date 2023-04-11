package snw.kookbc.impl.command.cloud;

import org.jetbrains.annotations.Nullable;
import snw.jkook.command.JKookCommand;
import snw.jkook.plugin.Plugin;
import snw.kookbc.impl.command.CommandManagerImpl;
import snw.kookbc.impl.command.CommandMap;
import snw.kookbc.impl.command.WrappedCommand;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 2023/4/11<br>
 * KookBC<br>
 *
 * @author huanmeng_qwq
 */
public class CloudCommandMap implements CommandMap {
    protected final Map<String, WrappedCommand> commandsWithoutPrefix = new ConcurrentHashMap<>();
    protected final Map<String, WrappedCommand> commandsWithPrefix = new ConcurrentHashMap<>();
    protected final Map<String, WrappedCommand> commandsWithoutPrefixView = Collections.unmodifiableMap(commandsWithoutPrefix);
    protected final Map<String, WrappedCommand> commandsWithPrefixView = Collections.unmodifiableMap(commandsWithPrefix);

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
        WrappedCommand wrapped = new WrappedCommand(command, plugin);

        commandsWithoutPrefix.put(command.getRootName(), wrapped);
        for (String head : createHeaders(command)) {
            commandsWithPrefix.put(head, wrapped);
        }
        this.cloudCommandManager.registerJKook(command, plugin, this.commandManager);
    }

    public void unregister(JKookCommand command) {
        commandsWithPrefix.entrySet().removeIf(i -> i.getValue().getCommand() == command);
        commandsWithoutPrefix.entrySet().removeIf(i -> i.getValue().getCommand() == command);
        cloudCommandManager.unregisterJKookCommand(command);
    }

    public void unregisterAll(Plugin plugin) {
        commandsWithPrefix.entrySet().removeIf(i -> i.getValue().getPlugin() == plugin);
        commandsWithoutPrefix.entrySet().removeIf(i -> i.getValue().getPlugin() == plugin);
        cloudCommandManager.unregisterJKookCommands(plugin);
    }

    @Override
    public void clear() {
        commandsWithPrefix.clear();
        commandsWithoutPrefix.clear();
    }

    public Map<String, WrappedCommand> getView(boolean withPrefix) {
        return withPrefix ? commandsWithPrefixView : commandsWithoutPrefixView;
    }

    public @Nullable JKookCommand getByRootName(String head, boolean withPrefix) {
        Map<String, WrappedCommand> view = getView(withPrefix);
        if (view.containsKey(head)) {
            return view.get(head).getCommand();
        }
        return null;
    }


    protected static Collection<String> createHeaders(JKookCommand command) {
        Collection<String> result = new ArrayList<>(command.getPrefixes().size() * (command.getAliases().size() + 1));
        for (String prefix : command.getPrefixes()) {
            result.add(prefix + command.getRootName());
            for (String alias : command.getAliases()) {
                result.add(prefix + alias);
            }
        }
        return result;
    }

    public CloudCommandManagerImpl cloudCommandManager() {
        return cloudCommandManager;
    }
}
