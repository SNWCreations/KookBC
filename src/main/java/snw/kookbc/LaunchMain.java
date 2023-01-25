package snw.kookbc;

import snw.kookbc.impl.launch.Launch;

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
    public static void main(String[] args) {
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
