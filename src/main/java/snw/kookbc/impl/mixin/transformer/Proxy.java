package snw.kookbc.impl.mixin.transformer;

import org.spongepowered.asm.mixin.transformer.MixinTransformer;
import org.spongepowered.asm.service.ILegacyClassTransformer;
import org.spongepowered.asm.service.MixinService;
import snw.kookbc.impl.launch.IClassTransformer;

import java.util.ArrayList;
import java.util.List;

/**
 * Proxy transformer for the mixin transformer. These transformers are used
 * to allow the mixin transformer to be re-registered in the transformer
 * chain at a later stage in startup without having to fully re-initialise
 * the mixin transformer itself. Only the latest proxy to be instantiated
 * will actually provide callbacks to the underlying mixin transformer.
 */
public final class Proxy implements IClassTransformer, ILegacyClassTransformer {

    /**
     * All existing proxies
     */
    private static List<Proxy> proxies = new ArrayList<Proxy>();

    /**
     * Actual mixin transformer instance
     */
    private static MixinTransformer transformer = new MixinTransformer();

    /**
     * True if this is the active proxy, newer proxies disable their older
     * siblings
     */
    private boolean isActive = true;

    public Proxy() {
        for (Proxy proxy : Proxy.proxies) {
            proxy.isActive = false;
        }

        Proxy.proxies.add(this);
        MixinService.getService().getLogger("mixin").debug("Adding new mixin transformer proxy #{}", Proxy.proxies.size());
    }

    @Override
    public byte[] transform(String name, String transformedName, byte[] basicClass) {
        if (this.isActive) {
            return Proxy.transformer.transformClassBytes(name, transformedName, basicClass);
        }

        return basicClass;
    }

    @Override
    public String getName() {
        return this.getClass().getName();
    }

    @Override
    public boolean isDelegationExcluded() {
        return true;
    }

    @Override
    public byte[] transformClassBytes(String name, String transformedName, byte[] basicClass) {
        if (this.isActive) {
            return Proxy.transformer.transformClassBytes(name, transformedName, basicClass);
        }

        return basicClass;
    }

}
