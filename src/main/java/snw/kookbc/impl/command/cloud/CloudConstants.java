/*
 *     KookBC -- The Kook Bot Client & JKook API standard implementation for Java.
 *     Copyright (C) 2022 - 2023 KookBC contributors
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU Affero General Public License as published
 *     by the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU Affero General Public License for more details.
 *
 *     You should have received a copy of the GNU Affero General Public License
 *     along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package snw.kookbc.impl.command.cloud;

import cloud.commandframework.keys.CloudKey;
import cloud.commandframework.keys.SimpleCloudKey;
import cloud.commandframework.meta.CommandMeta;
import io.leangen.geantyref.TypeToken;
import snw.jkook.message.Message;
import snw.jkook.plugin.Plugin;

import java.util.Collection;

/**
 * @author huanmeng_qwq
 */
public final class CloudConstants {
    public static final CommandMeta.Key<Plugin> PLUGIN_KEY = CommandMeta.Key.of(Plugin.class, "kookbc_plugin");
    public static final CommandMeta.Key<Collection<String>> PREFIX_KEY = CommandMeta.Key.of(new TypeToken<Collection<String>>() {
    }, "kookbc_prefixes");
    public static final CommandMeta.Key<Collection<String>> ALIAS_KEY = CommandMeta.Key.of(new TypeToken<Collection<String>>() {
    }, "kookbc_aliases");
    public static final CommandMeta.Key<Boolean> JKOOK_COMMAND_KEY = CommandMeta.Key.of(Boolean.class, "is_jkook_command");
    public static final CommandMeta.Key<String> HELP_CONTENT_KEY = CommandMeta.Key.of(String.class, "jkook_help_content");
    public static final CloudKey<Message> KOOK_MESSAGE_KEY = SimpleCloudKey.of("kook_message", TypeToken.get(Message.class));

    private CloudConstants() {
    }
}
