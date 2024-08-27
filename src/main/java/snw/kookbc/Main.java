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

import static snw.kookbc.util.Util.isStartByLaunch;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.lang.management.RuntimeMXBean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import joptsimple.OptionException;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;
import snw.jkook.JKook;
import snw.jkook.config.InvalidConfigurationException;
import snw.jkook.config.file.YamlConfiguration;
import snw.kookbc.impl.CoreImpl;
import snw.kookbc.impl.KBCClient;
import snw.kookbc.impl.command.CommandManagerImpl;
import snw.kookbc.launcher.Launcher;
import uk.org.lidalia.sysoutslf4j.context.SysOutOverSLF4J;

public class Main extends Launcher {
    private static final String MAIN_THREAD_NAME = "Main Thread";
    private static final Logger logger = LoggerFactory.getLogger(Main.class);
    private static final File kbcLocal = new File("kbc.yml");

    public static void main(String[] args) {
        try {
            System.exit(main0(args));
        } catch (Throwable e) {
            logger.error("Unexpected situation happened during the execution of main method!", e);
            System.exit(1);
        }
    }

    public Main() {
        super();
    }

    private static int main0(String[] args) {
        return new Main().start(args);
    }

    private int start(String[] args) {
        Thread.currentThread().setName(MAIN_THREAD_NAME);
        SysOutOverSLF4J.registerLoggingSystem("org.apache.logging");
        SysOutOverSLF4J.sendSystemOutAndErrToSLF4J();

        onSetup();

        // KBC accepts following arguments:
        // --token <tokenValue>   --  Use the tokenValue as the token
        // --help                 --  Get help and exit

        OptionParser parser = new OptionParser();
        OptionSpec<String> tokenOption = parser.accepts("token", "The token that will be used. (Unsafe, write token to kbc.yml instead.)").withOptionalArg();
        OptionSpec<Void> helpOption = parser.accepts("help", "Get help and exit.");

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
                return 1;
            }
            return 0;
        }

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
        return startClient(token, config, pluginsFolder);
    }

    protected int startClient(String token, YamlConfiguration config, File pluginsFolder) {
        if (!isStartByLaunch()) {
            logger.warn("***************************************");
            logger.warn("Launching KookBC WITHOUT Mixin support!");
            logger.warn("All Mixins in plugins will be ignored.");
            logger.warn("Tips: You can safely ignore this if you");
            logger.warn(" don't have Mixin plugins.");
            logger.warn("***************************************");
        }
        RuntimeMXBean runtimeMX = ManagementFactory.getRuntimeMXBean();
        OperatingSystemMXBean osMX = ManagementFactory.getOperatingSystemMXBean();
        if (runtimeMX != null && osMX != null) {
            logger.debug("System information is following:");
            logger.debug("Java: {} ({} {} by {})", runtimeMX.getSpecVersion(), runtimeMX.getVmName(), runtimeMX.getVmVersion(), runtimeMX.getVmVendor());
            logger.debug("Host: {} {} (Architecture: {})", osMX.getName(), osMX.getVersion(), osMX.getArch());
        } else {
            logger.debug("Unable to read system info");
        }

        CoreImpl core = new CoreImpl(logger);
        JKook.setCore(core);
        KBCClient client = new KBCClient(core, config, pluginsFolder, token, CommandManagerImpl::new,
                null, null, null, null, null, null);

        // make sure the things can stop correctly (e.g. Scheduler), but the crash makes no sense.
        Runtime.getRuntime().addShutdownHook(new Thread(client::shutdown, "JVM Shutdown Hook Thread"));

        try {
            client.start();
        } catch (Exception e) {
            logger.error("Failed to start client", e);
            client.shutdown();
            return 1;
        }

        try {
            client.loop();
            client.waitUntilShutdown();
        } finally {
            client.shutdown();
        }
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

