package snw.kookbc.impl.mixin;

import org.spongepowered.asm.service.IMixinServiceBootstrap;
import snw.kookbc.impl.launch.Launch;

public class KookBCServiceBootstrap implements IMixinServiceBootstrap {

    private static final String SERVICE_PACKAGE = "org.spongepowered.asm.service.";
    private static final String LAUNCH_PACKAGE = "org.spongepowered.asm.launch.";
    private static final String LOGGING_PACKAGE = "org.spongepowered.asm.logging.";

    private static final String MIXIN_UTIL_PACKAGE = "org.spongepowered.asm.util.";
    private static final String LEGACY_ASM_PACKAGE = "org.spongepowered.asm.lib.";
    private static final String ASM_PACKAGE = "org.objectweb.asm.";
    private static final String MIXIN_PACKAGE = "org.spongepowered.asm.mixin.";

    @Override
    public String getName() {
        return "KookBC";
    }

    @Override
    public String getServiceClassName() {
        return "snw.kookbc.impl.mixin.MixinServiceKookBC";
    }

    @Override
    public void bootstrap() {
        // Essential ones
        Launch.classLoader.addClassLoaderExclusion(KookBCServiceBootstrap.SERVICE_PACKAGE);
        Launch.classLoader.addClassLoaderExclusion(KookBCServiceBootstrap.LAUNCH_PACKAGE);
        Launch.classLoader.addClassLoaderExclusion(KookBCServiceBootstrap.LOGGING_PACKAGE);

        // Important ones
        Launch.classLoader.addClassLoaderExclusion(KookBCServiceBootstrap.ASM_PACKAGE);
        Launch.classLoader.addClassLoaderExclusion(KookBCServiceBootstrap.LEGACY_ASM_PACKAGE);
        Launch.classLoader.addClassLoaderExclusion(KookBCServiceBootstrap.MIXIN_PACKAGE);
        Launch.classLoader.addClassLoaderExclusion(KookBCServiceBootstrap.MIXIN_UTIL_PACKAGE);
    }
}
