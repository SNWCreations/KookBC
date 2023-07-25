package snw.kookbc.impl.command.cloud.exception;

import cloud.commandframework.context.CommandContext;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;
import snw.jkook.plugin.Plugin;

/**
 * 2023/7/23<br>
 * KookBC<br>
 *
 * @author huanmeng_qwq
 */
public class CommandPluginDisabledException extends IllegalArgumentException {
    private static final long serialVersionUID = 5973116780767261548L;

    private final CommandContext<?> commandContext;
    private final Plugin plugin;

    public CommandPluginDisabledException(@NonNull Throwable cause, @Nullable CommandContext<?> commandContext, Plugin plugin) {
        super(cause);
        this.commandContext = commandContext;
        this.plugin = plugin;
    }

    public Plugin getPlugin() {
        return plugin;
    }

    public CommandContext<?> getCommandContext() {
        return commandContext;
    }
}
