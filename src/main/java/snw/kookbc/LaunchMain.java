package snw.kookbc;

import snw.kookbc.impl.launch.Launch;
import snw.kookbc.impl.launch.LogWrapper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

// Invoke the main method under this class to launch KookBC with Mixin support.
// author: huanmeng_qwq
// added since 2023/1/24
// WARNING: Do not use this class in embedded environments!
public class LaunchMain {
    // We won't use Main#MAIN_THREAD_NAME
    // The Main class should not be loaded at this time
    private static final String MAIN_THREAD_NAME = "Main Thread";

    public static void main(String[] args) {
        Thread.currentThread().setName(MAIN_THREAD_NAME);
        LogWrapper.LOGGER.info("Launching KookBC with Mixin support");
        LogWrapper.LOGGER.info("The author of Mixin support: huanmeng_qwq@Github"); // thank you!  --- SNWCreations
        // Turn to true if you are debugging and want to see the full debug log from Mixin
        // After that, add -Dlog4j2.log.level=debug to VM options for full debug log!
        launch(Arrays.asList(args), false);
    }

    public static void launch(List<String> args, boolean fullDebug) {
        System.setProperty("kookbc.launch", "true");
        if (fullDebug) {
            System.setProperty("mixin.debug", "true");
        }
        if (args.stream().noneMatch(e -> e.contains("--tweakClass"))) {
            args = new ArrayList<>(args);

            args.add("--tweakClass");
            args.add("snw.kookbc.impl.mixin.MixinTweaker");
        }
        Launch.main(args.toArray(new String[0]));
    }
}
