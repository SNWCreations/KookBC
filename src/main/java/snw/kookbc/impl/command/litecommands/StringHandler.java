package snw.kookbc.impl.command.litecommands;

import dev.rollczi.litecommands.handler.result.ResultHandler;
import dev.rollczi.litecommands.handler.result.ResultHandlerChain;
import dev.rollczi.litecommands.invocation.Invocation;
import snw.jkook.command.CommandSender;
import snw.jkook.command.ConsoleCommandSender;
import snw.jkook.entity.User;
import snw.jkook.message.Message;

class StringHandler implements ResultHandler<CommandSender, String> {

    @Override
    public void handle(Invocation<CommandSender> invocation, String result, ResultHandlerChain<CommandSender> chain) {
        CommandSender sender = invocation.sender();
        Message message = null;
        try {
            message = invocation.context().get(Message.class).orElse(null);
        } catch (NullPointerException ignored) {
        }
        // Core core = invocation.context().get(Core.class).orElseThrow(() -> new IllegalStateException("Core is not present"));
        if (sender instanceof User) {
            if (message != null) {
                message.reply(result);
            }
        } else if (sender instanceof ConsoleCommandSender) {
            ((ConsoleCommandSender) sender).getLogger().info("命令 {} 的执行结果: {}", invocation.name(), result);
        } else {
            throw new IllegalStateException("Unknown sender type: " + sender.getClass().getName());
        }
    }

}