package snw.kookbc.impl.command.cloud.parser;

import cloud.commandframework.arguments.parser.ArgumentParseResult;
import cloud.commandframework.arguments.parser.ArgumentParser;
import cloud.commandframework.context.CommandContext;
import cloud.commandframework.exceptions.parsing.NoInputProvidedException;
import org.checkerframework.checker.nullness.qual.NonNull;
import snw.jkook.command.CommandException;
import snw.jkook.command.CommandSender;
import snw.jkook.entity.User;
import snw.kookbc.impl.KBCClient;

import java.util.Queue;

/**
 * 2023/6/26<br>
 * KookBC<br>
 *
 * @author huanmeng_qwq
 */
public class UserArgumentParser implements ArgumentParser<CommandSender, User> {
    private final KBCClient client;

    public UserArgumentParser(KBCClient client) {
        this.client = client;
    }

    @Override
    public @NonNull ArgumentParseResult<@NonNull User> parse(@NonNull CommandContext<@NonNull CommandSender> commandContext, @NonNull Queue<@NonNull String> inputQueue) {
        final String input = inputQueue.peek();
        if (input == null) {
            return ArgumentParseResult.failure(new NoInputProvidedException(UserArgumentParser.class, commandContext));
        }
        try {
            User user = client.getCore().getHttpAPI().getUser(input);
            if (user == null) {
                return ArgumentParseResult.failure(new CommandException("User not found"));
            }
            inputQueue.remove();
            return ArgumentParseResult.success(user);
        } catch (final Exception e) {
            return ArgumentParseResult.failure(new CommandException("User not found"));
        }
    }
}
