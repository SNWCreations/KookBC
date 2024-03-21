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

package snw.kookbc.util;

import snw.jkook.plugin.PluginDescription;

import java.util.Comparator;

import static snw.kookbc.util.Util.anyContains;

public final class DependencyListBasedPluginDescriptionComparator implements Comparator<PluginDescription> {
    public static final DependencyListBasedPluginDescriptionComparator INSTANCE
            = new DependencyListBasedPluginDescriptionComparator();

    @Override
    public int compare(PluginDescription o1, PluginDescription o2) {
        //noinspection ComparatorMethodParameterNotUsed - SHOULDN'T RETURN 0 IN THIS IMPLEMENTATION
        return anyContains(o2.getName(), o1.getDepend(), o1.getSoftDepend()) ? 1 : -1;
    }

    private DependencyListBasedPluginDescriptionComparator() {
    }
}
