/*
 * This file is part of Mixin, licensed under the MIT License (MIT).
 *
 * Copyright (c) SpongePowered <https://www.spongepowered.org>
 * Copyright (c) contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package snw.kookbc.impl.mixin;

import org.jetbrains.annotations.ApiStatus;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.launch.GlobalProperties;
import org.spongepowered.asm.launch.GlobalProperties.Keys;
import org.spongepowered.asm.launch.platform.MainAttributes;
import org.spongepowered.asm.launch.platform.container.ContainerHandleURI;
import org.spongepowered.asm.launch.platform.container.ContainerHandleVirtual;
import org.spongepowered.asm.launch.platform.container.IContainerHandle;
import org.spongepowered.asm.logging.ILogger;
import org.spongepowered.asm.mixin.MixinEnvironment.CompatibilityLevel;
import org.spongepowered.asm.mixin.MixinEnvironment.Phase;
import org.spongepowered.asm.mixin.throwables.MixinException;
import org.spongepowered.asm.service.*;
import org.spongepowered.asm.transformers.MixinClassReader;
import org.spongepowered.asm.util.Constants;
import org.spongepowered.asm.util.Files;
import org.spongepowered.asm.util.perf.Profiler;
import org.spongepowered.asm.util.perf.Profiler.Section;
import org.spongepowered.include.com.google.common.collect.ImmutableList;
import org.spongepowered.include.com.google.common.collect.Sets;
import org.spongepowered.include.com.google.common.io.ByteStreams;
import org.spongepowered.include.com.google.common.io.Closeables;
import snw.kookbc.impl.launch.IClassNameTransformer;
import snw.kookbc.impl.launch.IClassTransformer;
import snw.kookbc.LaunchMain;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;

/**
 * Mixin service for launchwrapper
 */
public class MixinServiceKookBC extends MixinServiceAbstract implements IClassProvider, IClassBytecodeProvider, ITransformerProvider {

    // Blackboard keys
    public static final Keys BLACKBOARD_KEY_TWEAKCLASSES = Keys.of("TweakClasses");
    @SuppressWarnings("unused")
    public static final Keys BLACKBOARD_KEY_TWEAKS = Keys.of("Tweaks");
    public static final Keys MAIN_THREAD_NAME = Keys.of("mainThreadName");

    private static final String MIXIN_TWEAKER_CLASS = MixinServiceAbstract.LAUNCH_PACKAGE + "MixinTweaker";

    // Consts
    private static final String STATE_TWEAKER = "snw.kookbc.impl.mixin.EnvironmentStateTweaker";
    private static final String TRANSFORMER_PROXY_CLASS = "snw.kookbc.impl.mixin.transformer.Proxy";

    /**
     * Known re-entrant transformers, other re-entrant transformers will
     * detected automatically
     */
    private static final Set<String> excludeTransformers = Sets.newHashSet();

    /**
     * Log4j2 logger
     */
    private static final Logger logger = LoggerFactory.getLogger(MixinServiceKookBC.class);

    /**
     * Utility for reflecting into Launch ClassLoader
     */
    private final LaunchClassLoaderUtil classLoaderUtil;

    /**
     * Local transformer chain, this consists of all transformers present at the
     * init phase with the exclusion of the mixin transformer itself and known
     * re-entrant transformers. Detected re-entrant transformers will be
     * subsequently removed.
     */
    private List<ILegacyClassTransformer> delegatedTransformers;

    /**
     * Class name transformer (if present)
     */
    private IClassNameTransformer nameTransformer;

    public MixinServiceKookBC() {
        this.classLoaderUtil = new LaunchClassLoaderUtil(LaunchMain.classLoader);
    }

    @Override
    public String getName() {
        return "LaunchWrapper";
    }

    /* (non-Javadoc)
     * @see org.spongepowered.asm.service.IMixinService#isValid()
     */
    @Override
    public boolean isValid() {
        try {
            // Detect launchwrapper
            // noinspection ResultOfMethodCallIgnored
            LaunchMain.classLoader.hashCode();
        } catch (Throwable ex) {
            return false;
        }
        return true;
    }

    /* (non-Javadoc)
     * @see org.spongepowered.asm.service.IMixinService#prepare()
     */
    @Override
    public void prepare() {
        // Only needed in dev, in production this would be handled by the tweaker
        LaunchMain.classLoader.addClassLoaderExclusion(MixinServiceAbstract.LAUNCH_PACKAGE);
        GlobalProperties.put(MAIN_THREAD_NAME, Thread.currentThread().getName());
    }

    /* (non-Javadoc)
     * @see org.spongepowered.asm.service.IMixinService#getInitialPhase()
     */
    @Override
    public Phase getInitialPhase() {
        System.setProperty("mixin.env.remapRefMap", "false");

        if (MixinServiceKookBC.findInStackTrace("snw.kookbc.LaunchMain", "launch") > 132) {
            return Phase.DEFAULT;
        }
        return Phase.PREINIT;
    }

    /* (non-Javadoc)
     * @see org.spongepowered.asm.service.IMixinService
     *      #getMaxCompatibilityLevel()
     */
    @Override
    public CompatibilityLevel getMaxCompatibilityLevel() {
        return CompatibilityLevel.JAVA_8;
    }

    @Override
    protected ILogger createLogger(String name) {
        return new LoggerAdapterLog4j2(name);
    }

    /* (non-Javadoc)
     * @see org.spongepowered.asm.service.IMixinService#init()
     */
    @Override
    public void init() {
        int launch = MixinServiceKookBC.findInStackTrace("snw.kookbc.LaunchMain", "launch");
        if (launch < 4) {
            MixinServiceKookBC.logger.error("MixinBootstrap.doInit() called during a tweak constructor! {}", launch);
        }

        List<String> tweakClasses = GlobalProperties.get(MixinServiceKookBC.BLACKBOARD_KEY_TWEAKCLASSES);
        if (tweakClasses != null) {
            tweakClasses.add(MixinServiceKookBC.STATE_TWEAKER);
        }

        super.init();
    }

    /* (non-Javadoc)
     * @see org.spongepowered.asm.service.IMixinService#getPlatformAgents()
     */
    @Override
    public Collection<String> getPlatformAgents() {
        return ImmutableList.of(
                "snw.kookbc.impl.mixin.MixinPlatformAgentKookBC"
        );
    }

    @Override
    public IContainerHandle getPrimaryContainer() {
        URI uri;
        try {
            uri = this.getClass().getProtectionDomain().getCodeSource().getLocation().toURI();
            return new ContainerHandleURI(uri);
        } catch (URISyntaxException ex) {
            ex.printStackTrace();
        }
        return new ContainerHandleVirtual(this.getName());
    }

    @Override
    public Collection<IContainerHandle> getMixinContainers() {
        ImmutableList.Builder<IContainerHandle> list = ImmutableList.builder();
        this.getContainersFromClassPath(list);
        this.getContainersFromAgents(list);
        return list.build();
    }

    private void getContainersFromClassPath(ImmutableList.Builder<IContainerHandle> list) {
        // We know this is deprecated, it works for LW though, so access directly
        URL[] sources = this.getClassPath();
        if (sources != null) {
            for (URL url : sources) {
                try {
                    URI uri = url.toURI();
                    MixinServiceKookBC.logger.debug("Scanning {} for mixin tweaker", uri);
                    if (!"file".equals(uri.getScheme()) || !Files.toFile(uri).exists()) {
                        continue;
                    }
                    MainAttributes attributes = MainAttributes.of(uri);
                    String tweaker = attributes.get(Constants.ManifestAttributes.TWEAKER);
                    if (MixinServiceKookBC.MIXIN_TWEAKER_CLASS.equals(tweaker)) {
                        list.add(new ContainerHandleURI(uri));
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        }
    }

    /* (non-Javadoc)
     * @see org.spongepowered.asm.service.IMixinService#getClassProvider()
     */
    @Override
    public IClassProvider getClassProvider() {
        return this;
    }

    /* (non-Javadoc)
     * @see org.spongepowered.asm.service.IMixinService#getBytecodeProvider()
     */
    @Override
    public IClassBytecodeProvider getBytecodeProvider() {
        return this;
    }

    /* (non-Javadoc)
     * @see org.spongepowered.asm.service.IMixinService#getTransformerProvider()
     */
    @Override
    public ITransformerProvider getTransformerProvider() {
        return this;
    }

    /* (non-Javadoc)
     * @see org.spongepowered.asm.service.IMixinService#getClassTracker()
     */
    @Override
    public IClassTracker getClassTracker() {
        return this.classLoaderUtil;
    }

    /* (non-Javadoc)
     * @see org.spongepowered.asm.service.IMixinService#getAuditTrail()
     */
    @Override
    public IMixinAuditTrail getAuditTrail() {
        return null;
    }

    /* (non-Javadoc)
     * @see org.spongepowered.asm.service.IClassProvider#findClass(
     *      java.lang.String)
     */
    @Override
    public Class<?> findClass(String name) throws ClassNotFoundException {
        return LaunchMain.classLoader.findClass(name);
    }

    /* (non-Javadoc)
     * @see org.spongepowered.asm.service.IClassProvider#findClass(
     *      java.lang.String, boolean)
     */
    @Override
    public Class<?> findClass(String name, boolean initialize) throws ClassNotFoundException {
        return Class.forName(name, initialize, LaunchMain.classLoader);
    }

    /* (non-Javadoc)
     * @see org.spongepowered.asm.service.IClassProvider#findAgentClass(
     *      java.lang.String, boolean)
     */
    @Override
    public Class<?> findAgentClass(String name, boolean initialize) throws ClassNotFoundException {
        return Class.forName(name, initialize, LaunchMain.class.getClassLoader());
    }

    /* (non-Javadoc)
     * @see org.spongepowered.asm.service.IMixinService#beginPhase()
     */
    @Override
    public void beginPhase() {
        LaunchMain.classLoader.registerTransformer(MixinServiceKookBC.TRANSFORMER_PROXY_CLASS);
        this.delegatedTransformers = null;
    }

    /* (non-Javadoc)
     * @see org.spongepowered.asm.service.IMixinService#checkEnv(
     *      java.lang.Object)
     */
    @Override
    public void checkEnv(Object bootSource) {
        if (bootSource.getClass().getClassLoader() != LaunchMain.class.getClassLoader()) {
            throw new MixinException("Attempted to init the mixin environment in the wrong classloader");
        }
    }

    /* (non-Javadoc)
     * @see org.spongepowered.asm.service.IMixinService#getResourceAsStream(
     *      java.lang.String)
     */
    @Override
    public InputStream getResourceAsStream(String name) {
        return LaunchMain.classLoader.getResourceAsStream(name);
    }

    /* (non-Javadoc)
     * @see org.spongepowered.asm.service.IClassProvider#getClassPath()
     */
    @Override
    public URL[] getClassPath() {
        return LaunchMain.classLoader.getSources().toArray(new URL[0]);
    }

    /* (non-Javadoc)
     * @see org.spongepowered.asm.service.IMixinService#getTransformers()
     */
    @Override
    public Collection<ITransformer> getTransformers() {
        List<IClassTransformer> transformers = LaunchMain.classLoader.getTransformers();
        List<ITransformer> wrapped = new ArrayList<>(transformers.size());
        for (IClassTransformer transformer : transformers) {
            if (transformer instanceof ITransformer) {
                wrapped.add((ITransformer) transformer);
            } else {
                wrapped.add(new LegacyTransformerHandle(transformer));
            }

            if (transformer instanceof IClassNameTransformer) {
                MixinServiceKookBC.logger.debug("Found name transformer: {}", transformer.getClass().getName());
                this.nameTransformer = (IClassNameTransformer) transformer;
            }

        }
        return wrapped;
    }

    /**
     * Returns (and generates if necessary) the transformer delegation list for
     * this environment.
     *
     * @return current transformer delegation list (read-only)
     */
    @Override
    public List<ITransformer> getDelegatedTransformers() {
        return Collections.unmodifiableList(this.getDelegatedLegacyTransformers());
    }

    private List<ILegacyClassTransformer> getDelegatedLegacyTransformers() {
        if (this.delegatedTransformers == null) {
            this.buildTransformerDelegationList();
        }

        return this.delegatedTransformers;
    }

    /**
     * Builds the transformer list to apply to loaded mixin bytecode. Since
     * generating this list requires inspecting each transformer by name (to
     * cope with the new wrapper functionality added by FML) we generate the
     * list just once per environment and cache the result.
     */
    private void buildTransformerDelegationList() {
        MixinServiceKookBC.logger.debug("Rebuilding transformer delegation list:");
        this.delegatedTransformers = new ArrayList<>();
        for (ITransformer transformer : this.getTransformers()) {
            if (!(transformer instanceof ILegacyClassTransformer)) {
                continue;
            }

            ILegacyClassTransformer legacyTransformer = (ILegacyClassTransformer) transformer;
            String transformerName = legacyTransformer.getName();
            boolean include = true;
            for (String excludeClass : MixinServiceKookBC.excludeTransformers) {
                if (transformerName.contains(excludeClass)) {
                    include = false;
                    break;
                }
            }
            if (include && !legacyTransformer.isDelegationExcluded()) {
                MixinServiceKookBC.logger.debug("  Adding:    {}", transformerName);
                this.delegatedTransformers.add(legacyTransformer);
            } else {
                MixinServiceKookBC.logger.debug("  Excluding: {}", transformerName);
            }
        }

        MixinServiceKookBC.logger.debug("Transformer delegation list created with {} entries", this.delegatedTransformers.size());
    }

    /**
     * Adds a transformer to the transformer exclusions list
     *
     * @param name Class transformer exclusion to add
     */
    @Override
    public void addTransformerExclusion(String name) {
        MixinServiceKookBC.excludeTransformers.add(name);

        // Force rebuild of the list
        this.delegatedTransformers = null;
    }

    /**
     * Retrieve class bytes using available classloaders, does not transform the
     * class
     *
     * @param name            class name
     * @param transformedName transformed class name
     * @return class bytes or null if not found
     * @throws IOException propagated
     */
    @ApiStatus.Internal
    public byte[] getClassBytes(String name, String transformedName) throws IOException {
        byte[] classBytes = LaunchMain.classLoader.getClassBytes(name);
        if (classBytes != null) {
            return classBytes;
        }

        URLClassLoader appClassLoader;
        if (LaunchMain.class.getClassLoader() instanceof URLClassLoader) {
            appClassLoader = (URLClassLoader) LaunchMain.class.getClassLoader();
        } else {
            appClassLoader = new URLClassLoader(new URL[]{}, LaunchMain.class.getClassLoader());
        }

        InputStream classStream = null;
        try {
            final String resourcePath = transformedName.replace('.', '/').concat(".class");
            classStream = appClassLoader.getResourceAsStream(resourcePath);
            // NullPointerException will be caught in the following catch statement
            // noinspection DataFlowIssue
            return ByteStreams.toByteArray(classStream);
        } catch (Exception ex) {
            return null;
        } finally {
            Closeables.closeQuietly(classStream);
        }
    }

    /**
     * Loads class bytecode from the classpath
     *
     * @param className       Name of the class to load
     * @param runTransformers True to run the loaded bytecode through the
     *                        delegate transformer chain
     * @return Transformed class bytecode for the specified class
     * @throws ClassNotFoundException if the specified class could not be loaded
     * @throws IOException            if an error occurs whilst reading the specified class
     */
    @ApiStatus.Internal
    public byte[] getClassBytes(String className, boolean runTransformers) throws ClassNotFoundException, IOException {
        String transformedName = className.replace('/', '.');
        String name = this.unmapClassName(transformedName);

        Profiler profiler = Profiler.getProfiler("mixin");
        Section loadTime = profiler.begin(Profiler.ROOT, "class.load");
        byte[] classBytes = this.getClassBytes(name, transformedName);
        loadTime.end();

        if (runTransformers) {
            Section transformTime = profiler.begin(Profiler.ROOT, "class.transform");
            classBytes = this.applyTransformers(name, transformedName, classBytes, profiler);
            transformTime.end();
        }

        if (classBytes == null) {
            throw new ClassNotFoundException(String.format("The specified class '%s' was not found", transformedName));
        }

        return classBytes;
    }

    /**
     * Since we obtain the class bytes with getClassBytes(), we need to apply
     * the transformers ourself
     *
     * @param name            class name
     * @param transformedName transformed class name
     * @param basicClass      input class bytes
     * @return class bytecode after processing by all registered transformers
     * except the excluded transformers
     */
    private byte[] applyTransformers(String name, String transformedName, byte[] basicClass, Profiler profiler) {
        if (this.classLoaderUtil.isClassExcluded(name, transformedName)) {
            return basicClass;
        }

        for (ILegacyClassTransformer transformer : this.getDelegatedLegacyTransformers()) {
            // Clear the re-entrance semaphore
            this.lock.clear();

            int pos = transformer.getName().lastIndexOf('.');
            String simpleName = transformer.getName().substring(pos + 1);
            Section transformTime = profiler.begin(Profiler.FINE, simpleName.toLowerCase(Locale.ROOT));
            transformTime.setInfo(transformer.getName());
            basicClass = transformer.transformClassBytes(name, transformedName, basicClass);
            transformTime.end();

            if (this.lock.isSet()) {
                // Also add it to the exclusion list so we can exclude it if the environment triggers a rebuild
                this.addTransformerExclusion(transformer.getName());

                this.lock.clear();
                MixinServiceKookBC.logger.info("A re-entrant transformer '{}' was detected and will no longer process meta class data",
                        transformer.getName());
            }
        }

        return basicClass;
    }

    private String unmapClassName(String className) {
        if (this.nameTransformer == null) {
            this.findNameTransformer();
        }

        if (this.nameTransformer != null) {
            return this.nameTransformer.unmapClassName(className);
        }

        return className;
    }

    private void findNameTransformer() {
        List<IClassTransformer> transformers = LaunchMain.classLoader.getTransformers();
        for (IClassTransformer transformer : transformers) {
            if (transformer instanceof IClassNameTransformer) {
                MixinServiceKookBC.logger.debug("Found name transformer: {}", transformer.getClass().getName());
                this.nameTransformer = (IClassNameTransformer) transformer;
            }
        }
    }

    /* (non-Javadoc)
     * @see org.spongepowered.asm.service.IMixinService#getClassNode(
     *      java.lang.String)
     */
    @Override
    public ClassNode getClassNode(String className) throws ClassNotFoundException, IOException {
        return this.getClassNode(className, this.getClassBytes(className, true), ClassReader.EXPAND_FRAMES);
    }

    /* (non-Javadoc)
     * @see org.spongepowered.asm.service.IClassBytecodeProvider#getClassNode(
     *      java.lang.String, boolean)
     */
    @Override
    public ClassNode getClassNode(String className, boolean runTransformers) throws ClassNotFoundException, IOException {
        return this.getClassNode(className, this.getClassBytes(className, true), ClassReader.EXPAND_FRAMES);
    }

    /**
     * Gets an ASM Tree for the supplied class bytecode
     *
     * @param classBytes Class bytecode
     * @param flags      ClassReader flags
     * @return ASM Tree view of the specified class
     */
    @SuppressWarnings("SameParameterValue")
    @ApiStatus.Internal
    private ClassNode getClassNode(String className, byte[] classBytes, int flags) {
        ClassNode classNode = new ClassNode();
        ClassReader classReader = new MixinClassReader(classBytes, className);
        classReader.accept(classNode, flags);
        return classNode;
    }

    @SuppressWarnings("SameParameterValue")
    private static int findInStackTrace(String className, String methodName) {
        Thread currentThread = Thread.currentThread();

        if (!GlobalProperties.get(MAIN_THREAD_NAME, "main").equals(currentThread.getName())) {
            return 0;
        }

        StackTraceElement[] stackTrace = currentThread.getStackTrace();
        for (StackTraceElement s : stackTrace) {
            if (className.equals(s.getClassName()) && methodName.equals(s.getMethodName())) {
                return s.getLineNumber();
            }
        }

        return 0;
    }

}
