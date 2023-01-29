/*
 * License: https://github.com/Mojang/LegacyLauncher
 */
package snw.kookbc.impl.launch;

public interface IClassTransformer {

    byte[] transform(String name, String transformedName, byte[] basicClass);

}
