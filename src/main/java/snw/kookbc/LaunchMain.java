package snw.kookbc;

import snw.kookbc.impl.launch.Launch;
import snw.kookbc.impl.launch.LogWrapper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 2023/1/24<br>
 * KookBC<br>
 *
 * @author huanmeng_qwq
 */
public class LaunchMain {
    // We won't use Main#MAIN_THREAD_NAME
    // The Main class should not be loaded at this time
    private static final String MAIN_THREAD_NAME = "Main Thread";

    public static void main(String[] args) {
        Thread.currentThread().setName(MAIN_THREAD_NAME);
        LogWrapper.log.info("Launching KookBC with Mixin support");
        LogWrapper.log.info("The author of Mixin support: huanmeng_qwq@Github"); // thank you!  --- SNWCreations
        launch(Arrays.asList(args), true);
    }

    public static void launch(List<String> args, boolean verbose) {
        System.setProperty("kookbc.launch", "true");
        if (verbose) {
            System.setProperty("mixin.debug.verbose", "true");
        }
        if (args.stream().noneMatch(e -> e.contains("--tweakClass"))) {
            args = new ArrayList<>(args);

            args.add("--tweakClass");
            args.add("snw.kookbc.impl.mixin.MixinTweaker");
        }
        Launch.main(args.toArray(new String[0]));
    }
}
