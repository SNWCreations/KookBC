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

package snw.kookbc.impl.command.cloud;

import snw.jkook.plugin.Plugin;

import java.util.Arrays;

/**
 * @author huanmeng_qwq
 */
public class CloudCommandInfo {
    private final Plugin owningPlugin;
    private final String rootName;
    private final String syntax;
    private final String[] aliases;
    private final String[] prefixes;
    private final String description;
    private final String helpContent;
    private final boolean isJKookCommand;
    private final boolean hidden;

    public CloudCommandInfo(Plugin owningPlugin, String rootName, String syntax, String[] aliases, String[] prefixes, String description, String helpContent, boolean isJKookCommand, boolean hidden) {
        this.owningPlugin = owningPlugin;
        this.rootName = rootName;
        this.syntax = syntax;
        this.aliases = aliases;
        this.prefixes = prefixes;
        this.description = description;
        this.helpContent = helpContent;
        this.isJKookCommand = isJKookCommand;
        this.hidden = hidden;
    }

    public Plugin owningPlugin() {
        return owningPlugin;
    }

    public String rootName() {
        return rootName;
    }

    public String syntax() {
        return syntax;
    }

    public String[] aliases() {
        return aliases;
    }

    public String[] prefixes() {
        return prefixes;
    }

    public String description() {
        return description;
    }

    public String helpContent() {
        return helpContent;
    }

    public boolean isJKookCommand() {
        return isJKookCommand;
    }

    @Override
    public String toString() {
        return "CloudCommandInfo{" +
                "owningPlugin=" + owningPlugin.getDescription().getName() +
                ", rootName='" + rootName + '\'' +
                ", syntax='" + syntax + '\'' +
                ", aliases=" + Arrays.toString(aliases) +
                ", prefixes=" + Arrays.toString(prefixes) +
                ", description='" + description + '\'' +
                ", helpContent='" + helpContent + '\'' +
                ", isJKookCommand=" + isJKookCommand +
                ", hidden=" + hidden +
                '}';
    }

    public boolean hidden() {
        return hidden;
    }
}
