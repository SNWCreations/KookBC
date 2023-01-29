/*
 * License: https://github.com/Mojang/LegacyLauncher
 */
package snw.kookbc.impl.launch;

public interface IClassNameTransformer {

    String unmapClassName(String name);

    String remapClassName(String name);

}
