package snw.kookbc.impl.command.cloud;

import cloud.commandframework.keys.CloudKey;
import cloud.commandframework.keys.SimpleCloudKey;
import cloud.commandframework.meta.CommandMeta;
import io.leangen.geantyref.TypeToken;
import snw.jkook.message.Message;
import snw.jkook.plugin.Plugin;

import java.util.Collection;

/**
 * 2023/6/20<br>
 * KookBC<br>
 *
 * @author huanmeng_qwq
 */
public class CloudConstants {
    public static final CommandMeta.Key<Plugin> PLUGIN_KEY = CommandMeta.Key.of(Plugin.class, "kookbc_plugin");
    public static final CommandMeta.Key<Collection<String>> PREFIX_KEY = CommandMeta.Key.of(new TypeToken<Collection<String>>() {
    }, "kookbc_prefixes");
    public static final CommandMeta.Key<Collection<String>> ALIAS_KEY = CommandMeta.Key.of(new TypeToken<Collection<String>>() {
    }, "kookbc_aliases");
    public static final CommandMeta.Key<Boolean> JKOOK_COMMAND_KEY = CommandMeta.Key.of(Boolean.class, "is_jkook_command");
    public static final CommandMeta.Key<String> HELP_CONTENT_KEY = CommandMeta.Key.of(String.class, "jkook_help_content");
    public static final CloudKey<Message> KOOK_MESSAGE_KEY = SimpleCloudKey.of("kook_message", TypeToken.get(Message.class));

}
