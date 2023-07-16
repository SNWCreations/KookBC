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

package snw.kookbc.impl.launch;

import snw.kookbc.impl.plugin.MixinPluginManager;

import java.io.File;
import java.util.List;

// author: huanmeng_qwq
public class LaunchMainTweaker implements ITweaker {
    public static final String CLASS_NAME = "snw.kookbc.Main";
    private String[] args;

    @Override
    public void acceptOptions(List<String> args) {
        this.args = args.toArray(new String[0]);
    }

    @Override
    public void injectIntoClassLoader(LaunchClassLoader classLoader) {
        MixinPluginManager.instance().loadFolder(classLoader, new File("plugins"));
    }

    @Override
    public String getLaunchTarget() {
        return CLASS_NAME;
    }

    @Override
    public String[] getLaunchArguments() {
        if (args != null) {
            return args;
        }
        return new String[]{};
    }
}
