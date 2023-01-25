package snw.kookbc.impl.mixin;

import org.spongepowered.asm.launch.MixinBootstrap;
import org.spongepowered.asm.launch.platform.CommandLineOptions;
import snw.kookbc.impl.launch.ITweaker;
import snw.kookbc.impl.launch.LaunchClassLoader;
import uk.org.lidalia.sysoutslf4j.common.ReflectionUtils;

import java.util.List;


public class MixinTweaker implements ITweaker {
    private String[] args;

    /**
     * Hello world
     */
    public MixinTweaker() {
        ReflectionUtils.invokeStaticMethod("start", MixinBootstrap.class);
    }

    @Override
    public final void acceptOptions(List<String> args) {
        ReflectionUtils.invokeStaticMethod("doInit", MixinBootstrap.class, CommandLineOptions.class, CommandLineOptions.ofArgs(args));
        this.args = args.toArray(new String[0]);
    }

    @Override
    public final void injectIntoClassLoader(LaunchClassLoader classLoader) {
        MixinBootstrap.getPlatform().inject();
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
