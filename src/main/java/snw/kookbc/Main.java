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
import static snw.kookbc.util.VirtualThreadUtil.startVirtualThread;

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
            logger.error("主方法执行过程中发生意外情况！", e);
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
        OptionSpec<String> tokenOption = parser.accepts("token", "将要使用的 token。（不安全，建议将 token 写入 kbc.yml）").withOptionalArg();
        OptionSpec<Void> helpOption = parser.accepts("help", "获取帮助并退出");

        OptionSet options;
        try {
            options = parser.parse(args);
        } catch (OptionException e) {
            logger.error("无法解析命令行参数。参数格式是否正确？", e);
            return 1;
        }

        if (options == null || !options.hasOptions()) { // if it is null, then maybe the valid data is in kbc.yml
            options = parser.parse();
        }

        if (options.has(helpOption)) {
            try {
                parser.printHelpOn(System.out);
            } catch (IOException e) {
                logger.error("无法打印帮助信息。");
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
            logger.error("无法加载 kbc.yml 配置文件", e);
        }

        String configToken = config.getString("token");
        if (configToken != null && !configToken.isEmpty()) {
            logger.debug("在 kbc.yml 中找到有效的 token。");
            if (token == null || token.isEmpty()) {
                logger.debug("命令行中的 token 值无效。将使用 kbc.yml 配置文件中的值。");
                token = configToken;
            } else {
                logger.debug("命令行中的 token 值有效，将不使用 kbc.yml 配置文件中的值。");
            }
        } else {
            logger.warn("在 kbc.yml 中找到无效的 token 值。");
        }

        if (token == null) {
            logger.error("未提供 token。程序无法继续运行。");
            return 1;
        }

        if (!config.getBoolean("allow-help-ad", true)) {
            logger.warn("检测到 allow-help-ad 被设置为 false！ :("); // why don't you support us?
        }
        return startClient(token, config, pluginsFolder);
    }

    protected int startClient(String token, YamlConfiguration config, File pluginsFolder) {
        if (!isStartByLaunch()) {
            logger.warn("***************************************");
            logger.warn("正在启动 KookBC，但未提供 Mixin 支持！");
            logger.warn("插件中的所有 Mixin 将被忽略。");
            logger.warn("提示：如果您没有 Mixin 插件，");
            logger.warn(" 可以安全地忽略此消息。");
            logger.warn("***************************************");
        }
        RuntimeMXBean runtimeMX = ManagementFactory.getRuntimeMXBean();
        OperatingSystemMXBean osMX = ManagementFactory.getOperatingSystemMXBean();
        if (runtimeMX != null && osMX != null) {
            logger.debug("系统信息如下：");
            logger.debug("Java: {} ({} {} 由 {} 提供)", runtimeMX.getSpecVersion(), runtimeMX.getVmName(), runtimeMX.getVmVersion(), runtimeMX.getVmVendor());
            logger.debug("主机: {} {} (架构: {})", osMX.getName(), osMX.getVersion(), osMX.getArch());
        } else {
            logger.debug("无法读取系统信息");
        }

        CoreImpl core = new CoreImpl(logger);
        JKook.setCore(core);
        KBCClient client = new KBCClient(core, config, pluginsFolder, token, CommandManagerImpl::new,
                null, null, null, null, null, null);

        // make sure the things can stop correctly (e.g. Scheduler), but the crash makes no sense.
        Runtime.getRuntime().addShutdownHook(new Thread(client::shutdown, "JVM-Shutdown-Hook-Thread"));

        try {
            client.start();
        } catch (Exception e) {
            logger.error("启动客户端失败", e);
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
            logger.warn("由于发生错误，无法保存 kbc.yml 文件", e);
        }
    }
}

