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

package snw.kookbc.util;

import org.yaml.snakeyaml.Yaml;
import snw.jkook.command.JKookCommand;
import snw.jkook.plugin.Plugin;
import snw.jkook.plugin.PluginDescription;
import snw.jkook.plugin.PluginLoader;
import snw.jkook.util.Validate;
import snw.kookbc.impl.KBCClient;
import snw.kookbc.impl.command.CommandManagerImpl;
import snw.kookbc.impl.command.WrappedCommand;
import snw.kookbc.impl.command.cloud.CloudCommandInfo;
import snw.kookbc.impl.command.cloud.CloudCommandManagerImpl;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.zip.DataFormatException;
import java.util.zip.Inflater;

public class Util {

    private Util() {
    } // cannot call constructor

    // -1 = Outdated
    // 0 = Latest
    // 1 = From Future (means this version is not on GitHub. Development version?)
    public static int getVersionDifference(String current, String versionToCompare) {
        if (current.equals(versionToCompare))
            return 0;
        if (current.split("\\.").length != 3 || versionToCompare.split("\\.").length != 3)
            return -1;

        int curMaj = Integer.parseInt(current.split("\\.")[0]);
        int curMin = Integer.parseInt(current.split("\\.")[1]);
        String curPatch = current.split("\\.")[2];

        int relMaj = Integer.parseInt(versionToCompare.split("\\.")[0]);
        int relMin = Integer.parseInt(versionToCompare.split("\\.")[1]);
        String relPatch = versionToCompare.split("\\.")[2];

        if (curMaj < relMaj)
            return -1;
        if (curMaj > relMaj)
            return 1;
        if (curMin < relMin)
            return -1;
        if (curMin > relMin)
            return 1;

        // Detect snapshot (if exists)
        int curPatchN = Integer.parseInt(curPatch.split("-")[0]);
        int relPatchN = Integer.parseInt(relPatch.split("-")[0]);
        if (curPatchN < relPatchN)
            return -1;
        if (curPatchN > relPatchN)
            return 1;
        if (!relPatch.contains("-") && curPatch.contains("-"))
            return -1;
        if (relPatch.contains("-") && curPatch.contains("-"))
            return 0;

        return 1;
    }

    public static String toEnglishNumOrder(int num) {
        String numStr = String.valueOf(num);
        String suffix;
        switch (Integer.parseInt(String.valueOf(numStr.charAt(numStr.length() - 1)))) {
            case 1:
                suffix = "st";
                break;
            case 2:
                suffix = "nd";
                break;
            case 3:
                suffix = "rd";
                break;
            default:
                suffix = "th";
                break;
        }
        return numStr + suffix;
    }

    public static void pluginNotNull(Plugin plugin) {
        Validate.notNull(plugin, "The provided plugin is null");
    }

    public static void ensurePluginEnabled(Plugin plugin) {
        pluginNotNull(plugin);
        Validate.isTrue(plugin.isEnabled(), "The plugin is disabled");
    }

    public static byte[] decompressDeflate(byte[] data) throws IOException, DataFormatException {
        Inflater decompressor = new Inflater();
        decompressor.reset();
        decompressor.setInput(data);

        try (ByteArrayOutputStream o = new ByteArrayOutputStream(data.length)) {
            byte[] buf = new byte[1024];
            while (!decompressor.finished()) {
                int i = decompressor.inflate(buf);
                o.write(buf, 0, i);
            }
            return o.toByteArray();
        } finally {
            decompressor.end();
        }
    }

    public static byte[] inputStreamToByteArray(InputStream input) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        byte[] buffer = new byte[4096];
        int n;
        while (-1 != (n = input.read(buffer))) {
            output.write(buffer, 0, n);
        }
        return output.toByteArray();
    }

    public static boolean isStartByLaunch() {
        return Boolean.getBoolean("kookbc.launch");
    }

    public static void closeLoaderIfPossible(Plugin plugin) {
        final ClassLoader classLoader = plugin.getClass().getClassLoader();
        if (classLoader instanceof AutoCloseable) {
            closeTheAutoCloseable((AutoCloseable) classLoader);
        }
    }

    public static void closeLoaderIfPossible(PluginLoader loader) {
        if (loader instanceof AutoCloseable) {
            closeTheAutoCloseable((AutoCloseable) loader);
        }
    }

    public static void closeTheAutoCloseable(AutoCloseable closeable) {
        try {
            closeable.close();
        } catch (Exception ignored) {
        }
    }

    public static List<String> listCommandsHelp(KBCClient client) {
        CommandManagerImpl commandManager = (CommandManagerImpl) client.getCore().getCommandManager();
        JKookCommand[] commands = commandManager.getCommandSet().toArray(new JKookCommand[0]);
        List<String> result = new LinkedList<>();
        for (JKookCommand command : commands) {
            insertCommandHelpContent(result, command.getRootName(), command.getPrefixes(), command.getDescription());
        }
        return result;
    }

    public static List<String> listCloudCommandsHelp(KBCClient client) {
        List<CloudCommandInfo> commandsInfo = ((CloudCommandManagerImpl) client.getCore().getCommandManager()).getCommandsInfo();

        List<String> result = new LinkedList<>();
        for (CloudCommandInfo command : commandsInfo) {
            insertCommandHelpContent(result, command.syntax(), Arrays.asList(command.prefixes()), command.description());
        }
        return result;
    }

    private static void insertCommandHelpContent(List<String> result, String rootName, Collection<String> prefixes, String description) {
        result.add(
                limit(
                        String.format("(%s)%s: %s",
                                String.join(" ",
                                        prefixes),
                                rootName,
                                (isBlank(description)) ? "此命令没有简介。" : description
                        ),
                        4997
                )
        );
    }

    public static JKookCommand findSpecificCommand(KBCClient client, String name) {
        CommandManagerImpl commandManager = (CommandManagerImpl) client.getCore().getCommandManager();
        if (name != null && !name.isEmpty()) {
            WrappedCommand command = commandManager.getCommand(name);
            if (command == null) {
                return null;
            }
            return command.getCommand();
        } else {
            return null;
        }
    }

    public static CloudCommandInfo findSpecificCloudCommand(KBCClient client, String name) {
        List<CloudCommandInfo> commandsInfo = ((CloudCommandManagerImpl) client.getCore().getCommandManager()).getCommandsInfo();
        if (!isBlank(name)) {
            return commandsInfo.stream()
                    .filter(info ->
                            info.rootName().equalsIgnoreCase(name) ||
                                    info.syntax().equalsIgnoreCase(name) ||
                                    Arrays.stream(info.aliases())
                                            .anyMatch(
                                                    alias -> alias.equalsIgnoreCase(name)
                                            )
                    ).findFirst().orElse(null);
        } else {
            return null;
        }
    }

    public static String limit(String original, int maxLength) {
        if (maxLength < 0 || original.length() <= maxLength)
            return original;
        return String.format("%s...", original.substring(0, maxLength));
    }


    public static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    public static PluginDescription createDescription(InputStream stream) {
        final Yaml parser = new Yaml();
        try {
            final Map<String, Object> ymlContent = parser.load(stream);
            // noinspection unchecked
            return new PluginDescription(
                    Objects.requireNonNull(ymlContent.get("name"), "name is missing").toString(),
                    Objects.requireNonNull(ymlContent.get("version"), "version is missing").toString(),
                    Objects.requireNonNull(ymlContent.get("api-version"), "api-version is missing").toString(),
                    ymlContent.getOrDefault("description", "").toString(),
                    ymlContent.getOrDefault("website", "").toString(),
                    Objects.requireNonNull(ymlContent.get("main"), "main is missing").toString(),
                    (List<String>) ymlContent.getOrDefault("authors", Collections.emptyList()),
                    (List<String>) ymlContent.getOrDefault("depend", Collections.emptyList()),
                    (List<String>) ymlContent.getOrDefault("softdepend", Collections.emptyList())
            );
        } catch (ClassCastException e) {
            throw new IllegalArgumentException("Invalid plugin.yml", e);
        }
    }
}
