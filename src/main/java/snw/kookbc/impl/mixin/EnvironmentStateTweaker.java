package snw.kookbc.impl.mixin;

import org.spongepowered.asm.launch.MixinBootstrap;
import org.spongepowered.asm.mixin.MixinEnvironment;
import org.spongepowered.asm.mixin.MixinEnvironment.Phase;
import snw.kookbc.impl.launch.ITweaker;
import snw.kookbc.impl.launch.LaunchClassLoader;
import uk.org.lidalia.sysoutslf4j.common.ReflectionUtils;

import java.util.List;

/**
 * Tweaker used to notify the environment when we transition from preinit to
 * default
 */
public class EnvironmentStateTweaker implements ITweaker {

    @Override
    public void acceptOptions(List<String> args) {
    }

    @Override
    public void injectIntoClassLoader(LaunchClassLoader classLoader) {
        MixinBootstrap.getPlatform().inject();
    }

    @Override
    public String getLaunchTarget() {
        return "";
    }

    @Override
    public String[] getLaunchArguments() {
        ReflectionUtils.invokeStaticMethod("gotoPhase", MixinEnvironment.class, Phase.class, Phase.DEFAULT);
        return new String[0];
    }

}
