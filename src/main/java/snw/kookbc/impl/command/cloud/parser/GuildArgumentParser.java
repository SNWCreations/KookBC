package snw.kookbc.impl.command.cloud.parser;

import cloud.commandframework.arguments.parser.ArgumentParseResult;
import cloud.commandframework.arguments.parser.ArgumentParser;
import cloud.commandframework.context.CommandContext;
import org.checkerframework.checker.nullness.qual.NonNull;
import snw.jkook.command.CommandException;
import snw.jkook.command.CommandSender;
import snw.jkook.entity.Guild;
import snw.kookbc.impl.KBCClient;

import java.util.Queue;

/**
 * 2023/6/26<br>
 * KookBC<br>
 *
 * @author huanmeng_qwq
 */
public class GuildArgumentParser implements @NonNull ArgumentParser<CommandSender, Guild> {
    private final KBCClient client;

    public GuildArgumentParser(KBCClient client) {
        this.client = client;
    }

    @Override
    public @NonNull ArgumentParseResult<@NonNull Guild> parse(@NonNull CommandContext<@NonNull CommandSender> commandContext, @NonNull Queue<@NonNull String> inputQueue) {
        final String input = inputQueue.peek();
        if (input == null) {
            return ArgumentParseResult.failure(new cloud.commandframework.exceptions.parsing.NoInputProvidedException(GuildArgumentParser.class, commandContext));
        }
        try {
            Guild guild = client.getCore().getHttpAPI().getGuild(input);
            if (guild == null) {
                return ArgumentParseResult.failure(new CommandException("Guild not found"));
            }
            inputQueue.remove();
            return ArgumentParseResult.success(guild);
        } catch (final Exception e) {
            return ArgumentParseResult.failure(new CommandException("Guild not found"));
        }
    }
}
