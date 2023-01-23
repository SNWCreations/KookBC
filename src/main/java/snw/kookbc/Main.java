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

package snw.kookbc;

import joptsimple.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import snw.jkook.JKook;
import snw.jkook.config.InvalidConfigurationException;
import snw.jkook.config.file.YamlConfiguration;
import snw.kookbc.impl.CoreImpl;
import snw.kookbc.impl.KBCClient;
import snw.kookbc.impl.launch.Launch;
import snw.kookbc.impl.network.webhook.WebHookClient;
import uk.org.lidalia.sysoutslf4j.context.SysOutOverSLF4J;

import java.io.*;
import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.lang.management.RuntimeMXBean;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Main {
    public static final String MAIN_THREAD_NAME = "Main Thread";
    private static final Logger logger = LoggerFactory.getLogger(Main.class);
    private static final File kbcLocal = new File("kbc.yml");

    public static void main(String[] args) {
        loadLaunch(Arrays.asList(args));
    }

    private static void loadLaunch(List<String> args) {
//        {
//            try {
//                Class.forName("snw.kookbc.impl.mixin.Blackboard");
//                Class.forName("snw.kookbc.impl.mixin.MixinServiceKookBC");
//                Class.forName("snw.kookbc.impl.mixin.KookBCServiceBootstrap");
//            } catch (ClassNotFoundException e) {
//                throw new RuntimeException(e);
//            }
//        }
        System.setProperty("mixin.debug.verbose", "true");
        if (args.stream().noneMatch(e -> e.contains("--tweakClass"))) {
            args = new ArrayList<>(args);

            args.add("--tweakClass");
            args.add("snw.kookbc.impl.mixin.MixinTweaker");
        }
        Launch.main(args.toArray(new String[0]));
    }


    public static class Main0 {
        public static int main(String[] args) {
            // KBC accepts following arguments:
            // --token <tokenValue>   --  Use the tokenValue as the token
            // --help                 --  Get help and exit

            OptionParser parser = new OptionParser();
            OptionSpec<String> tokenOption = parser.accepts("token", "The token that will be used. (Unsafe, write token to kbc.yml instead.)").withOptionalArg();
            OptionSpec<Void> helpOption = parser.accepts("help", "Get help and exit.");
            NonOptionArgumentSpec<String> nonOptions = parser.nonOptions();

            OptionSet options;
            try {
                options = parser.parse(args);
            } catch (OptionException e) {
                logger.error("Unable to parse argument. Is your argument correct?", e);
                return 1;
            }

            if (options == null || !options.hasOptions()) { // if it is null, then maybe the valid data is in kbc.yml
                options = parser.parse();
            }

            if (options.has(helpOption)) {
                try {
                    parser.printHelpOn(System.out);
                } catch (IOException e) {
                    logger.error("Unable to print help.");
                }
                return 1;
            }
            Thread.currentThread().setName(MAIN_THREAD_NAME);
            SysOutOverSLF4J.registerLoggingSystem("org.apache.logging");
            SysOutOverSLF4J.sendSystemOutAndErrToSLF4J();

            String token = options.valueOf(tokenOption);

            saveKBCConfig();
            File pluginsFolder = new File("plugins");
            if (!pluginsFolder.isDirectory()) {
                //noinspection ResultOfMethodCallIgnored
                pluginsFolder.mkdir();
            }

            YamlConfiguration config = new YamlConfiguration();

            try {
                config.load(kbcLocal);
            } catch (FileNotFoundException ignored) {
            } catch (IOException | InvalidConfigurationException e) {
                logger.error("Cannot load kbc.yml", e);
            }

            String configToken = config.getString("token");
            if (configToken != null && !configToken.isEmpty()) {
                logger.debug("Got valid token in kbc.yml.");
                if (token == null || token.isEmpty()) {
                    logger.debug("The value of token from command line is invalid. We will use the value from kbc.yml configuration.");
                    token = configToken;
                } else {
                    logger.debug("The value of token from command line is OK, so we won't use the value from kbc.yml configuration.");
                }
            } else {
                logger.warn("Invalid token value in kbc.yml.");
            }

            if (token == null) {
                logger.error("No token provided. Program cannot continue.");
                return 1;
            }

            if (!config.getBoolean("allow-help-ad", true)) {
                logger.warn("Detected allow-help-ad is false! :("); // why don't you support us?
            }
            return main1(token, config, pluginsFolder);
        }

        private static int main1(String token, YamlConfiguration config, File pluginsFolder) {
            RuntimeMXBean runtimeMX = ManagementFactory.getRuntimeMXBean();
            OperatingSystemMXBean osMX = ManagementFactory.getOperatingSystemMXBean();
            if (runtimeMX != null && osMX != null) {
                logger.debug("System information is following:");
                logger.debug("Java: {} ({} {})", runtimeMX.getSpecVersion(), runtimeMX.getVmName(), runtimeMX.getVmVersion());
                logger.debug("Host: {} {} (Architecture: {})", osMX.getName(), osMX.getVersion(), osMX.getArch());
            } else {
                logger.debug("Unable to read system info");
            }

            CoreImpl core = new CoreImpl(logger);
            JKook.setCore(core);
            KBCClient client;
            String mode = config.getString("mode");
            if (mode != null) {
                if (mode.equalsIgnoreCase("webhook")) {
                    client = new WebHookClient(core, config, pluginsFolder, token);
                } else {
                    client = new KBCClient(core, config, pluginsFolder, token);
                }
            } else {
                throw new IllegalArgumentException("Unknown network mode!");
            }

            // make sure the things can stop correctly (e.g. Scheduler), but the crash makes no sense.
            Runtime.getRuntime().addShutdownHook(new Thread(client::shutdown, "JVM Shutdown Hook Thread"));

            try {
                client.start();
            } catch (Exception e) {
                logger.error("Failed to start client", e);
                client.shutdown();
                return 1;
            }
            client.loop();
            client.shutdown();
            return 0;
        }

        private static void saveKBCConfig() {
            try (final InputStream stream = Main.class.getResourceAsStream("/kbc.yml")) {
                if (stream == null) {
                    throw new Error("Unable to find kbc.yml");
                }

                if (kbcLocal.exists()) {
                    return;
                }
                //noinspection ResultOfMethodCallIgnored
                kbcLocal.createNewFile();

                try (final FileOutputStream out = new FileOutputStream(kbcLocal)) {
                    int index;
                    byte[] bytes = new byte[1024];
                    while ((index = stream.read(bytes)) != -1) {
                        out.write(bytes, 0, index);
                    }
                }
            } catch (IOException e) {
                logger.warn("Cannot save kbc.yml because an error occurred", e);
            }
        }
    }

}
