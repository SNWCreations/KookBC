package snw.kookbc.impl.command.litecommands.internal;

import dev.rollczi.litecommands.annotations.command.Command;
import dev.rollczi.litecommands.annotations.context.Context;
import dev.rollczi.litecommands.annotations.description.Description;
import dev.rollczi.litecommands.annotations.execute.Execute;
import dev.rollczi.litecommands.annotations.inject.Inject;
import snw.jkook.command.CommandSender;
import snw.jkook.entity.User;
import snw.jkook.message.Message;
import snw.kookbc.impl.KBCClient;

@Command(name = "stop")
public class StopCommand {
    private final KBCClient client;

    @Inject
    public StopCommand(KBCClient client) {
        this.client = client;
    }

    @Execute
    @Description("停止实例。")
    public Object executeStop(@Context CommandSender sender, @Context Message message) {
        // Execute the /stop
        if (sender instanceof User) {
            if (client.getConfig().getBoolean("ignore-remote-call-invisible-internal-command", true)) {
                return null;
            }
            if (message != null) {
                return "你不能这样做，因为你正在尝试执行仅后台可用的命令。";
            }
        } else {
            client.shutdown();
        }
        return null;
    }

}
