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
