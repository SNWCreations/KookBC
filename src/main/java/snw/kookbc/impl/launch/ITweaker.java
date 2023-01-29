/*
 * License: https://github.com/Mojang/LegacyLauncher
 */
package snw.kookbc.impl.launch;

import java.util.List;

public interface ITweaker {

    void acceptOptions(List<String> args);

    void injectIntoClassLoader(LaunchClassLoader classLoader);

    String getLaunchTarget();

    String[] getLaunchArguments();

}
