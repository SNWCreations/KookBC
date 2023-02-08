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
package snw.kookbc.impl.mixin;

import org.spongepowered.asm.launch.MixinBootstrap;
import org.spongepowered.asm.launch.platform.CommandLineOptions;
import snw.kookbc.impl.launch.ITweaker;
import snw.kookbc.impl.launch.LaunchClassLoader;
import uk.org.lidalia.sysoutslf4j.common.ReflectionUtils;

import java.util.List;


@SuppressWarnings("unused")
public class MixinTweaker implements ITweaker {

    /**
     * Hello world
     */
    public MixinTweaker() {
        ReflectionUtils.invokeStaticMethod("start", MixinBootstrap.class);
    }

    @Override
    public final void acceptOptions(List<String> args) {
        ReflectionUtils.invokeStaticMethod("doInit", MixinBootstrap.class, CommandLineOptions.class, CommandLineOptions.ofArgs(args));
    }

    @Override
    public final void injectIntoClassLoader(LaunchClassLoader classLoader) {
        MixinBootstrap.getPlatform().inject();
    }

    @Override
    public String getLaunchTarget() {
        return null;
    }

    @Override
    public String[] getLaunchArguments() {
        return null;
    }

}
