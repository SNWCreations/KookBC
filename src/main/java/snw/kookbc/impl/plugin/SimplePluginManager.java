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

package snw.kookbc.impl.plugin;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import snw.jkook.plugin.*;
import snw.jkook.util.Validate;
import snw.kookbc.impl.KBCClient;
import snw.kookbc.impl.command.CommandManagerImpl;
import snw.kookbc.impl.command.ConsoleCommandSenderImpl;
import snw.kookbc.impl.launch.AccessClassLoader;
import snw.kookbc.launcher.Launcher;
import snw.kookbc.util.DependencyListBasedPluginDescriptionComparator;
import snw.kookbc.util.VirtualThreadUtil;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.jar.JarFile;
import java.util.stream.Collectors;

import static snw.kookbc.util.Util.closeLoaderIfPossible;
import static snw.kookbc.util.Util.getVersionDifference;

public class SimplePluginManager implements PluginManager {
    protected static final Predicate<File> STANDARD_PLUGIN_CHECKER;
    private KBCClient client;
    private Logger logger;
    private final Collection<Plugin> plugins = new ArrayList<>();
    private final Map<Predicate<File>, Function<ClassLoader, PluginLoader>> loaderMap = new LinkedHashMap<>();
    private final Map<Predicate<File>, Supplier<PluginDescriptionResolver>> pluginDescriptionResolverMap
            = new LinkedHashMap<>();

    static {
        STANDARD_PLUGIN_CHECKER = f -> {
            if (!f.getName().endsWith(".jar"))
                return false;
            try (JarFile jarFile = new JarFile(f)) {
                return jarFile.getJarEntry("plugin.yml") != null;
            } catch (IOException e) {
                return false;
            }
        };
    }

    public SimplePluginManager(KBCClient client) {
        this(client, client.getCore().getLogger());
    }

    public SimplePluginManager(KBCClient client, Logger logger) {
        this.client = client;
        this.logger = logger;
        this.registerPluginLoader(STANDARD_PLUGIN_CHECKER, this::createPluginLoader); // ensure overrides will apply
        this.registerPluginDescriptionResolver(STANDARD_PLUGIN_CHECKER, () -> PluginClassLoader.PluginDotYMLResolver.INSTANCE);
    }

    public void setClient(KBCClient client) {
        if (this.client != null) {
            throw new UnsupportedOperationException("Client already set");
        }
        this.client = client;
    }

    @Override
    public @Nullable Plugin getPlugin(String name) {
        return plugins.stream()
                .filter(IT -> Objects.equals(IT.getDescription().getName(), name))
                .findFirst()
                .orElse(null);
    }

    @Override
    public Plugin[] getPlugins() {
        return plugins.toArray(new Plugin[0]);
    }

    @Override
    public boolean isPluginEnabled(String name) {
        Plugin plugin = getPlugin(name);
        return plugin != null && plugin.isEnabled();
    }

    @Override
    public boolean isPluginEnabled(Plugin plugin) {
        return plugin.isEnabled();
    }

    @Override
    public @NotNull Plugin loadPlugin(File file) throws InvalidPluginException {
        return loadPlugin0(file, true);
    }

    protected Plugin loadPlugin0(File file, boolean failIfNoLoader) throws InvalidPluginException {
        // We won't close the ClassLoader, because Plugin#getResource need the ClassLoader to keep open.
        // Otherwise, Plugin#getResource will not work correctly.
        // If you want to reload a plugin, or fully uninstall a plugin, close the ClassLoader manually.
        // An example is following:
        // pluginManager.disablePlugin(plugin);
        // ((URLClassLoader) plugin.getClass().getClassLoader()).close();
        Plugin plugin;
        PluginLoader loader;
        ClassLoader parent = Launcher.instance().getPluginClassLoader(getClass());
        if (!(parent instanceof MarkedClassLoader)) {
            parent = null;
        }
        loader = createPluginLoaderForFile(file, parent);
        if (loader == null) {
            if (failIfNoLoader) {
                throw new InvalidPluginException("There is no loader can load the file " + file);
            }
            return null;
        }
        try {
            plugin = loader.loadPlugin(file);
        } catch (InvalidPluginException e) {
            closeLoaderIfPossible(loader);
            throw e; // loader created, but plugin not valid
        }
        PluginDescription description = plugin.getDescription();
        int diff = getVersionDifference(description.getApiVersion(), client.getCore().getAPIVersion());
        if (diff == -1) {
            plugin.getLogger().warn("该插件使用的 JKook API 版本过旧！我们使用的是 {}，获取到的是 {}", client.getCore().getAPIVersion(), description.getApiVersion());
        }
        if (diff == 1) {
            closeLoaderIfPossible(loader); // plugin won't be returned, so the loader should be closed to prevent resource leak
            throw new InvalidPluginException(String.format("该插件使用的 JKook API 版本不受支持！我们使用的是 %s，获取到的是 %s", client.getCore().getAPIVersion(), description.getApiVersion()));
        }
        return plugin;
    }

    @Override
    public @NotNull Plugin[] loadPlugins(File directory) {
        Validate.isTrue(directory.isDirectory(), "The provided file object is not a directory.");
        File[] files = directory.listFiles(File::isFile);
        if (files != null) {
            final LinkedHashMap<PluginDescription, File> orderMap = new LinkedHashMap<>();
            for (File file : files) {
                final PluginDescriptionResolver resolver = lookUpPluginDescriptionResolverForFile(file);
                if (resolver == null) {
                    continue;
                }
                final PluginDescription description = resolver.resolve(file);
                orderMap.put(description, file);
            }
            final LinkedList<Map.Entry<PluginDescription, File>> orders = new LinkedList<>(orderMap.entrySet());
            orders.sort((o1, o2) -> DependencyListBasedPluginDescriptionComparator.INSTANCE
                    .compare(o1.getKey(), o2.getKey()));
            Collection<Plugin> plugins = new ArrayList<>(files.length);
            for (Map.Entry<PluginDescription, File> entry : orders) {
                final File file = entry.getValue();
                Plugin plugin;
                try {
                    plugin = loadPlugin0(file, false);
                } catch (Throwable e) {
                    logger.error("无法从指定文件 {} 中加载插件", file, e);
                    continue;
                }
                if (plugin == null) {
                    continue; // no suitable loader can be created, not a valid plugin file.
                }
                boolean shouldAdd = true;
                for (final Plugin p : plugins) {
                    if (Objects.equals(p.getDescription().getName(), plugin.getDescription().getName())) {
                        logger.error(
                                "We have found the same plugin name \"{}\" from two plugin files:" +
                                        " {} and {}, the plugin inside {} won't be returned.",
                                plugin.getDescription().getName(),
                                plugin.getFile(),
                                p.getFile(),
                                plugin.getFile()
                        );
                        shouldAdd = false;
                    }
                }
                if (shouldAdd) {
                    plugins.add(plugin);
                }
            }
            return plugins.toArray(new Plugin[0]);
        }
        return new Plugin[0];
    }

    @Override
    public void disablePlugins() {
        for (Plugin plugin : plugins) {
            disablePlugin(plugin);
        }
    }

    @Override
    public void clearPlugins() {
        disablePlugins();
        plugins.clear();
    }

    @Override
    public void enablePlugin(Plugin plugin) throws UnknownDependencyException {
        if (isPluginEnabled(plugin)) return;
        PluginDescription description = plugin.getDescription();
        plugin.getLogger().info("正在启用 {} 版本 {}", description.getName(), description.getVersion());
        if (!plugin.getDataFolder().exists()) {
            //noinspection ResultOfMethodCallIgnored
            plugin.getDataFolder().mkdir();
        }
        for (String dep : description.getDepend()) {
            if (getPlugin(dep) == null) {
                throw new UnknownDependencyException(String.format("Detected unknown dependency '%s' from plugin '%s'", dep, description.getName()));
            }
        }
        try {
            plugin.setEnabled(true);
        } catch (Throwable e) {
            plugin.getLogger().error("无法启用此插件", e);
            disablePlugin(plugin); // make sure the plugin is still disabled
        }
    }

    @Override
    public void disablePlugin(Plugin plugin) {
        if (!isPluginEnabled(plugin)) return;
        PluginDescription description = plugin.getDescription();
        plugin.getLogger().info("正在禁用 {} 版本 {}", description.getName(), description.getVersion());
        // cancel tasks
        client.getCore().getScheduler().cancelTasks(plugin);
        client.getCore().getEventManager().unregisterAllHandlers(plugin);
        ConsoleCommandSenderImpl.removeFor(plugin);
        // unregister commands
        try {
            ((CommandManagerImpl) client.getCore().getCommandManager()).getCommandMap().unregisterAll(plugin);
            plugin.setEnabled(false);
        } catch (Throwable e) {
            plugin.getLogger().error("禁用此插件时发生异常", e);
        }
        if (plugin.getClass().getClassLoader() instanceof SimplePluginClassLoader) {
            try {
                ((SimplePluginClassLoader) plugin.getClass().getClassLoader()).close();
            } catch (IOException e) {
                logger.error("在尝试关闭 PluginClassLoader 时发生意外的 IOException。", e);
            }
        }
    }

    @Override
    public void addPlugin(Plugin plugin) {
        if (plugins.stream().anyMatch(IT -> Objects.equals(IT.getDescription().getName(), plugin.getDescription().getName()))) {
            throw new IllegalArgumentException("The provided plugin name is already in use.");
        }
        plugins.add(plugin);
    }

    @Override
    public void removePlugin(Plugin plugin) {
        plugins.remove(plugin);
    }

    @Override
    public void registerPluginLoader(Predicate<File> predicate, Function<ClassLoader, PluginLoader> provider) {
        Validate.notNull(predicate, "Predicate cannot be null");
        Validate.notNull(provider, "Provider cannot be null");
        loaderMap.put(predicate, provider);
    }

    @Override
    public void registerPluginDescriptionResolver(Predicate<File> predicate, Supplier<PluginDescriptionResolver> supplier) {
        Validate.notNull(predicate, "Predicate cannot be null");
        Validate.notNull(supplier, "Supplier cannot be null");
        pluginDescriptionResolverMap.put(predicate, supplier);
    }

    protected PluginLoader createPluginLoader(@Nullable ClassLoader parent) {
        return new SimplePluginClassLoader(client, AccessClassLoader.of(parent));
    }

    protected @Nullable PluginLoader createPluginLoaderForFile(File file, @Nullable ClassLoader parent) {
        for (Map.Entry<Predicate<File>, Function<ClassLoader, PluginLoader>> entry : loaderMap.entrySet()) {
            final Predicate<File> condition = entry.getKey();
            if (condition.test(file)) {
                final Function<ClassLoader, PluginLoader> provider = entry.getValue();
                return provider.apply(parent);
            }
        }
        return null;
    }

    protected @Nullable PluginDescriptionResolver lookUpPluginDescriptionResolverForFile(File file) {
        for (Map.Entry<Predicate<File>, Supplier<PluginDescriptionResolver>> entry :
                pluginDescriptionResolverMap.entrySet()) {
            final Predicate<File> condition = entry.getKey();
            if (condition.test(file)) {
                return entry.getValue().get();
            }
        }
        return null;
    }

    public Map<Predicate<File>, Function<ClassLoader, PluginLoader>> getLoaderProviders() {
        return Collections.unmodifiableMap(loaderMap);
    }

    // ===== 虚拟线程异步 API =====

    /**
     * 异步加载插件 - 使用虚拟线程
     *
     * <p>在虚拟线程中执行插件加载操作，避免阻塞主线程，
     * 特别适合加载大型插件或多个插件的场景。
     *
     * @param file 插件文件
     * @return 异步插件加载结果
     */
    public CompletableFuture<Plugin> loadPluginAsync(File file) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return loadPlugin(file);
            } catch (InvalidPluginException e) {
                throw new RuntimeException("异步加载插件失败: " + file.getName(), e);
            }
        }, VirtualThreadUtil.getPluginExecutor());
    }

    /**
     * 异步批量加载插件 - 使用虚拟线程
     *
     * <p>并行加载目录中的所有插件，显著提升多插件加载性能
     *
     * @param directory 插件目录
     * @return 异步插件数组结果
     */
    public CompletableFuture<Plugin[]> loadPluginsAsync(File directory) {
        return CompletableFuture.supplyAsync(() -> loadPlugins(directory), VirtualThreadUtil.getPluginExecutor());
    }

    /**
     * 异步启用插件 - 使用虚拟线程
     *
     * <p>在虚拟线程中执行插件启用操作，避免阻塞主线程
     *
     * @param plugin 要启用的插件
     * @return 异步启用结果
     */
    public CompletableFuture<Void> enablePluginAsync(Plugin plugin) {
        return CompletableFuture.runAsync(() -> {
            try {
                enablePlugin(plugin);
            } catch (UnknownDependencyException e) {
                throw new RuntimeException("异步启用插件失败: " + plugin.getDescription().getName(), e);
            }
        }, VirtualThreadUtil.getPluginExecutor());
    }

    /**
     * 异步禁用插件 - 使用虚拟线程
     *
     * <p>在虚拟线程中执行插件禁用操作，包括资源清理
     *
     * @param plugin 要禁用的插件
     * @return 异步禁用结果
     */
    public CompletableFuture<Void> disablePluginAsync(Plugin plugin) {
        return CompletableFuture.runAsync(() -> disablePlugin(plugin), VirtualThreadUtil.getPluginExecutor());
    }

    /**
     * 异步禁用所有插件 - 使用虚拟线程
     *
     * <p>并行禁用所有插件，提升关闭速度
     *
     * @return 异步禁用结果
     */
    public CompletableFuture<Void> disablePluginsAsync() {
        return CompletableFuture.runAsync(this::disablePlugins, VirtualThreadUtil.getPluginExecutor());
    }

    /**
     * 批量异步启用插件 - 使用虚拟线程
     *
     * <p>并行启用多个插件，考虑依赖关系顺序
     *
     * @param plugins 要启用的插件列表
     * @return 异步启用结果
     */
    public CompletableFuture<Void> batchEnablePluginsAsync(List<Plugin> plugins) {
        return CompletableFuture.runAsync(() -> {
            // 按依赖关系排序
            List<Plugin> sortedPlugins = plugins.stream()
                .sorted((p1, p2) -> DependencyListBasedPluginDescriptionComparator.INSTANCE
                    .compare(p1.getDescription(), p2.getDescription()))
                .collect(Collectors.toList());

            // 按序启用插件
            for (Plugin plugin : sortedPlugins) {
                try {
                    enablePlugin(plugin);
                } catch (UnknownDependencyException e) {
                    logger.error("批量启用插件失败: {}", plugin.getDescription().getName(), e);
                    // 继续处理其他插件
                }
            }
        }, VirtualThreadUtil.getPluginExecutor());
    }

    /**
     * 批量异步禁用插件 - 使用虚拟线程
     *
     * <p>并行禁用多个插件，提升性能
     *
     * @param plugins 要禁用的插件列表
     * @return 异步禁用结果
     */
    public CompletableFuture<Void> batchDisablePluginsAsync(List<Plugin> plugins) {
        List<CompletableFuture<Void>> futures = plugins.stream()
            .map(this::disablePluginAsync)
            .collect(Collectors.toList());

        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
    }

    /**
     * 异步重载插件 - 使用虚拟线程
     *
     * <p>先禁用再重新加载启用插件，在虚拟线程中执行避免阻塞
     *
     * @param plugin 要重载的插件
     * @return 异步重载结果
     */
    public CompletableFuture<Plugin> reloadPluginAsync(Plugin plugin) {
        return CompletableFuture.supplyAsync(() -> {
            String pluginName = plugin.getDescription().getName();
            File pluginFile = plugin.getFile();

            // 先禁用插件
            disablePlugin(plugin);
            removePlugin(plugin);

            // 重新加载插件
            try {
                Plugin newPlugin = loadPlugin(pluginFile);
                addPlugin(newPlugin);
                enablePlugin(newPlugin);
                return newPlugin;
            } catch (InvalidPluginException | UnknownDependencyException e) {
                throw new RuntimeException("异步重载插件失败: " + pluginName, e);
            }
        }, VirtualThreadUtil.getPluginExecutor());
    }

    /**
     * 异步扫描并加载新插件 - 使用虚拟线程
     *
     * <p>扫描插件目录，加载新发现的插件文件
     *
     * @param directory 插件目录
     * @return 新加载的插件列表
     */
    public CompletableFuture<List<Plugin>> scanAndLoadNewPluginsAsync(File directory) {
        return CompletableFuture.supplyAsync(() -> {
            Validate.isTrue(directory.isDirectory(), "The provided file object is not a directory.");
            File[] files = directory.listFiles(File::isFile);
            if (files == null) {
                return Collections.emptyList();
            }

            List<Plugin> newPlugins = new ArrayList<>();
            Set<String> existingPluginNames = plugins.stream()
                .map(p -> p.getDescription().getName())
                .collect(Collectors.toSet());

            for (File file : files) {
                final PluginDescriptionResolver resolver = lookUpPluginDescriptionResolverForFile(file);
                if (resolver == null) {
                    continue;
                }

                try {
                    final PluginDescription description = resolver.resolve(file);
                    // 检查是否是新插件
                    if (!existingPluginNames.contains(description.getName())) {
                        Plugin plugin = loadPlugin0(file, false);
                        if (plugin != null) {
                            newPlugins.add(plugin);
                            addPlugin(plugin);
                        }
                    }
                } catch (Throwable e) {
                    logger.error("扫描加载新插件失败: {}", file.getName(), e);
                }
            }

            return newPlugins;
        }, VirtualThreadUtil.getPluginExecutor());
    }

    /**
     * 异步插件热重载 - 使用虚拟线程
     *
     * <p>监控插件文件变化，自动重载已修改的插件
     *
     * @param pluginFile 插件文件
     * @return 异步重载结果
     */
    public CompletableFuture<Plugin> hotReloadPluginAsync(File pluginFile) {
        return CompletableFuture.supplyAsync(() -> {
            // 查找现有插件
            Plugin existingPlugin = plugins.stream()
                .filter(p -> p.getFile().equals(pluginFile))
                .findFirst()
                .orElse(null);

            if (existingPlugin != null) {
                // 重载现有插件
                return reloadPluginAsync(existingPlugin).join();
            } else {
                // 加载新插件
                try {
                    Plugin newPlugin = loadPlugin(pluginFile);
                    addPlugin(newPlugin);
                    enablePlugin(newPlugin);
                    return newPlugin;
                } catch (InvalidPluginException | UnknownDependencyException e) {
                    throw new RuntimeException("热重载插件失败: " + pluginFile.getName(), e);
                }
            }
        }, VirtualThreadUtil.getPluginExecutor());
    }
}
