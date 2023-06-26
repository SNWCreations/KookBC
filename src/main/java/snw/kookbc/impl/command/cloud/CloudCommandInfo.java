package snw.kookbc.impl.command.cloud;

import snw.jkook.plugin.Plugin;

import java.util.Arrays;

/**
 * @author huanmeng_qwq
 */
public class CloudCommandInfo {
    private final Plugin owningPlugin;
    private final String rootName;
    private final String[] aliases;
    private final String[] prefixes;
    private final String description;
    private final String helpContent;
    private final boolean isJKookCommand;

    public CloudCommandInfo(Plugin owningPlugin, String rootName, String[] aliases, String[] prefixes, String description, String helpContent, boolean isJKookCommand) {
        this.owningPlugin = owningPlugin;
        this.rootName = rootName;
        this.aliases = aliases;
        this.prefixes = prefixes;
        this.description = description;
        this.helpContent = helpContent;
        this.isJKookCommand = isJKookCommand;
    }

    public Plugin owningPlugin() {
        return owningPlugin;
    }

    public String rootName() {
        return rootName;
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
                ", aliases=" + Arrays.toString(aliases) +
                ", prefixes=" + Arrays.toString(prefixes) +
                ", description='" + description + '\'' +
                ", helpContent='" + helpContent + '\'' +
                ", isJKookCommand=" + isJKookCommand +
                '}';
    }
}
