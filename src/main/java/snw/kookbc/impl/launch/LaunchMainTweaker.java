package snw.kookbc.impl.launch;

import java.util.List;

/**
 * 2023/2/8<br>
 * KookBC<br>
 *
 * @author huanmeng_qwq
 */
public class LaunchMainTweaker implements ITweaker {
    private String[] args;

    @Override
    public void acceptOptions(List<String> args) {
        this.args = args.toArray(new String[0]);
    }

    @Override
    public void injectIntoClassLoader(LaunchClassLoader classLoader) {
    }

    @Override
    public String getLaunchTarget() {
        return "snw.kookbc.Main";
    }

    @Override
    public String[] getLaunchArguments() {
        if (args != null) {
            return args;
        }
        return new String[]{};
    }
}
