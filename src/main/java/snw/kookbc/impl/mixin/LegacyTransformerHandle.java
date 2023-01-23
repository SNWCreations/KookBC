package snw.kookbc.impl.mixin;

import org.spongepowered.asm.service.IClassProvider;
import org.spongepowered.asm.service.ILegacyClassTransformer;
import org.spongepowered.asm.service.MixinService;
import snw.kookbc.impl.launch.IClassTransformer;

import java.lang.annotation.Annotation;

/**
 * A handle for a legacy {@link IClassTransformer} for processing as a legacy
 * transformer
 */
class LegacyTransformerHandle implements ILegacyClassTransformer {

    /**
     * Wrapped transformer
     */
    private final IClassTransformer transformer;

    LegacyTransformerHandle(IClassTransformer transformer) {
        this.transformer = transformer;
    }

    /* (non-Javadoc)
     * @see org.spongepowered.asm.service.ILegacyClassTransformer#getName()
     */
    @Override
    public String getName() {
        return this.transformer.getClass().getName();
    }

    /* (non-Javadoc)
     * @see org.spongepowered.asm.service.ILegacyClassTransformer
     *      #isDelegationExcluded()
     */
    @SuppressWarnings("unchecked")
    @Override
    public boolean isDelegationExcluded() {
        try {
            IClassProvider classProvider = MixinService.getService().getClassProvider();
            Class<? extends Annotation> clResource = (Class<? extends Annotation>) classProvider.findClass("javax.annotation.Resource");
            return this.transformer.getClass().getAnnotation(clResource) != null;
        } catch (ClassNotFoundException ex) {
            return false;
        }
    }

    /* (non-Javadoc)
     * @see org.spongepowered.asm.service.ILegacyClassTransformer
     *      #transformClassBytes(java.lang.String, java.lang.String, byte[])
     */
    @Override
    public byte[] transformClassBytes(String name, String transformedName, byte[] basicClass) {
        return this.transformer.transform(name, transformedName, basicClass);
    }

}
