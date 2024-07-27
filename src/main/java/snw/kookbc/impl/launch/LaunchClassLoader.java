/*
 * License: https://github.com/Mojang/LegacyLauncher
 */
package snw.kookbc.impl.launch;

import org.spongepowered.asm.util.JavaVersion;
import snw.jkook.plugin.MarkedClassLoader;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLConnection;
import java.security.CodeSigner;
import java.security.CodeSource;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.jar.Attributes;
import java.util.jar.Attributes.Name;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

public class LaunchClassLoader extends URLClassLoader implements MarkedClassLoader, AccessClassLoader {
    public static final int BUFFER_SIZE = 1 << 12;
    private final LinkedHashSet<URL> sources;
    private final ClassLoader parent = getClass().getClassLoader();

    private final List<IClassTransformer> transformers = new ArrayList<>(2);
    private final Map<String, Class<?>> cachedClasses = new ConcurrentHashMap<>();
    private final Set<String> invalidClasses = new HashSet<>(1000);

    private final Set<String> classLoaderExceptions = new HashSet<>();
    private final Set<String> transformerExceptions = new HashSet<>();
    private final Map<String, byte[]> resourceCache = new ConcurrentHashMap<>(1000);
    private final Set<String> negativeResourceCache = Collections.newSetFromMap(new ConcurrentHashMap<>());

    private IClassNameTransformer renameTransformer;

    private final ThreadLocal<byte[]> loadBuffer = new ThreadLocal<>();

    private final Function<String, Package> packageProvider;

    private static final MethodHandle GET_DEFINED_PACKAGE;
    private static final MethodHandle GET_SYSTEM_RESOURCE;

    private static final String[] RESERVED_NAMES = {"CON", "PRN", "AUX", "NUL", "COM1", "COM2", "COM3", "COM4", "COM5",
            "COM6", "COM7", "COM8", "COM9", "LPT1", "LPT2", "LPT3", "LPT4", "LPT5", "LPT6", "LPT7", "LPT8", "LPT9"};

    private static final boolean DEBUG = Boolean.parseBoolean(System.getProperty("mixin.debug", "false"));

    static {
        if (JavaVersion.current() >= JavaVersion.JAVA_9) {
            try {
                // Ignore this if you are working on Java 9 and later
                // noinspection JavaReflectionMemberAccess
                Method getDefinedPackageMethod = ClassLoader.class.getMethod("getDefinedPackage", String.class);
                MethodHandles.Lookup lookup = MethodHandles.lookup();
                GET_DEFINED_PACKAGE = lookup.unreflect(getDefinedPackageMethod);
                GET_SYSTEM_RESOURCE = lookup.unreflect(ClassLoader.class.getMethod("getSystemResource", String.class));
            } catch (Throwable e) {
                throw new Error("Unable to initialize LaunchClassLoader.getPackage0 environment.", e);
            }
        } else {
            GET_DEFINED_PACKAGE = null;
            GET_SYSTEM_RESOURCE = null;
        }
    }

    public LaunchClassLoader(URL[] sources) {
        super(sources, null);
        this.sources = new LinkedHashSet<>(Arrays.asList(sources));

        // classloader exclusions
        addClassLoaderExclusion("java.");
        addClassLoaderExclusion("javax.");
        addClassLoaderExclusion("sun.");
        addClassLoaderExclusion("org.lwjgl.");
        addClassLoaderExclusion("org.apache.logging.");
        addClassLoaderExclusion("org.apache.log4j.");
        addClassLoaderExclusion("org.slf4j");
        addClassLoaderExclusion("okhttp3.");
        addClassLoaderExclusion("uk.org.lidalia.sysoutslf4j.");
        addClassLoaderExclusion("org.spongepowered.asm.launch.");
        addClassLoaderExclusion("ch.qos.logback");
        addClassLoaderExclusion("snw.kookbc.impl.launch.");
        addClassLoaderExclusion("snw.kookbc.LaunchMain");
        addClassLoaderExclusion("snw.kookbc.launcher.Launcher");
        addClassLoaderExclusion("snw.jkook.plugin.MarkedClassLoader");
        addClassLoaderExclusion("net.minecrell.terminalconsole.");
        addClassLoaderExclusion("org.jline.");
        addClassLoaderExclusion("joptsimple.");
        addClassLoaderExclusion("com.sun.");

        // transformer exclusions
        addTransformerExclusion("argo.");
        addTransformerExclusion("org.objectweb.asm.");
        addTransformerExclusion("com.google.common.");
        addTransformerExclusion("org.bouncycastle.");

        if (GET_DEFINED_PACKAGE != null) {
            final MethodHandle gdpForThis = GET_DEFINED_PACKAGE.bindTo(this);
            packageProvider = name -> {
                try {
                    return ((Package) gdpForThis.invokeExact(name));
                } catch (Throwable e) {
                    throw new Error("Unhandled exception from ClassLoader.getDefinedPackage method.", e);

                }
            };
        } else {
            packageProvider = this::getPackage;
        }
    }

    public void registerTransformer(String transformerClassName) {
        try {
            IClassTransformer transformer = (IClassTransformer) loadClass(transformerClassName).getConstructor().newInstance();
            transformers.add(transformer);
            if (transformer instanceof IClassNameTransformer && renameTransformer == null) {
                renameTransformer = (IClassNameTransformer) transformer;
            }
        } catch (Exception e) {
            LogWrapper.LOGGER.error("A critical problem occurred registering the ASM transformer class {}", transformerClassName, e);
        }
    }

    @SuppressWarnings("UnusedAssignment")
    @Override
    public Class<?> findClass(final String name) throws ClassNotFoundException {
        if (invalidClasses.contains(name)) {
            throw new ClassNotFoundException(name);
        }

        for (final String exception : classLoaderExceptions) {
            if (name.startsWith(exception)) {
                try {
                    return parent.loadClass(name);
                } catch (ClassNotFoundException e) { // parent failed, let us try?
                    break;
                }
            }
        }

        if (cachedClasses.containsKey(name)) {
            return cachedClasses.get(name);
        }

        for (final String exception : transformerExceptions) {
            if (name.startsWith(exception)) {
                try {
                    final Class<?> clazz = super.findClass(name);
                    cachedClasses.put(name, clazz);
                    return clazz;
                } catch (ClassNotFoundException e) {
                    invalidClasses.add(name);
                    throw e;
                }
            }
        }

        try {
            final String transformedName = transformName(name);
            if (cachedClasses.containsKey(transformedName)) {
                return cachedClasses.get(transformedName);
            }

            final String untransformedName = untransformName(name);

            final int lastDot = untransformedName.lastIndexOf('.');
            final String packageName = lastDot == -1 ? "" : untransformedName.substring(0, lastDot);
            final String fileName = untransformedName.replace('.', '/').concat(".class");
            URLConnection urlConnection = findCodeSourceConnectionFor(fileName);

            CodeSigner[] signers = null;

            final byte[] classBytes = getClassBytes(untransformedName);
            if (classBytes == null) {
                invalidClasses.add(name);
                throw new ClassNotFoundException(name);
            }
            if (lastDot > -1 && !untransformedName.startsWith("snw.kookbc.impl.launch")) {
                if (urlConnection instanceof JarURLConnection) {
                    final JarURLConnection jarURLConnection = (JarURLConnection) urlConnection;
                    final JarFile jarFile = jarURLConnection.getJarFile();

                    if (jarFile != null && jarFile.getManifest() != null) {
                        final Manifest manifest = jarFile.getManifest();
                        final JarEntry entry = jarFile.getJarEntry(fileName);

                        Package pkg = getPackage0(packageName);
                        signers = entry.getCodeSigners();
                        if (pkg == null) {
                            pkg = definePackage(packageName, manifest, jarURLConnection.getJarFileURL());
                        } else {
                            if (pkg.isSealed() && !pkg.isSealed(jarURLConnection.getJarFileURL())) {
                                LogWrapper.LOGGER.warn("The jar file {} is trying to seal already secured path {}", jarFile.getName(), packageName);
                            } else if (isSealed(packageName, manifest)) {
                                LogWrapper.LOGGER.warn("The jar file {} has a security seal for path {}, but that path is defined and not secure", jarFile.getName(), packageName);
                            }
                        }
                    }
                } else {
                    Package pkg = getPackage0(packageName);
                    if (pkg == null) {
                        pkg = definePackage(packageName, null, null, null, null, null, null, null);
                    } else if (pkg.isSealed()) {
                        LogWrapper.LOGGER.warn("The URL {} is defining elements for sealed path {}", urlConnection == null ? "null" : urlConnection.getURL(), packageName);
                    }
                }
            }

            byte[] transformedClass = runTransformers(untransformedName, transformedName, classBytes);
            if (transformedClass == null) {
                LogWrapper.LOGGER.error(untransformedName + " fail#runTransformers");
                transformedClass = classBytes;
            }

            final CodeSource codeSource = urlConnection == null ? null : new CodeSource(urlConnection.getURL(), signers);
            final Class<?> clazz = defineClass(transformedName, transformedClass, 0, transformedClass.length, codeSource);
            cachedClasses.put(transformedName, clazz);
            return clazz;
        } catch (Throwable e) {
            ClassLoader ctxLoader = Thread.currentThread().getContextClassLoader();
            if (ctxLoader != null) {
                try {
                    final Class<?> clazz = ctxLoader.loadClass(name);
                    cachedClasses.put(name, clazz);
                    invalidClasses.remove(name);
                    return clazz;
                } catch (ClassNotFoundException ignored) {
                }
            }
            invalidClasses.add(name);
            if (DEBUG) {
                LogWrapper.LOGGER.error("Exception encountered attempting classloading of {}", name, e);
            }
            throw new ClassNotFoundException(name, e);
        }
    }

    @Override
    public Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        return super.loadClass(name, resolve);
    }

    private Package getPackage0(String name) {
        return packageProvider.apply(name);
    }

    private String untransformName(final String name) {
        if (renameTransformer != null) {
            return renameTransformer.unmapClassName(name);
        }

        return name;
    }

    private String transformName(final String name) {
        if (renameTransformer != null) {
            return renameTransformer.remapClassName(name);
        }

        return name;
    }

    private boolean isSealed(final String path, final Manifest manifest) {
        Attributes attributes = manifest.getAttributes(path);
        String sealed = null;
        if (attributes != null) {
            sealed = attributes.getValue(Name.SEALED);
        }

        if (sealed == null) {
            attributes = manifest.getMainAttributes();
            if (attributes != null) {
                sealed = attributes.getValue(Name.SEALED);
            }
        }
        return "true".equalsIgnoreCase(sealed);
    }

    private URLConnection findCodeSourceConnectionFor(final String name) {
        final URL resource = findResource(name);
        if (resource != null) {
            try {
                return resource.openConnection();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        return null;
    }

    private byte[] runTransformers(final String name, final String transformedName, byte[] basicClass) {
        if (DEBUG) {
            LogWrapper.LOGGER.warn("Beginning transform of {{} ({})} Start Length: {}", name, transformedName, (basicClass == null ? 0 : basicClass.length));
            for (final IClassTransformer transformer : transformers) {
                final String transName = transformer.getClass().getName();
                LogWrapper.LOGGER.warn("Before Transformer {{} ({})} {}: {}", name, transformedName, transName, (basicClass == null ? 0 : basicClass.length));
                basicClass = transformer.transform(name, transformedName, basicClass);
                LogWrapper.LOGGER.warn("After  Transformer {{} ({})} {}: {}", name, transformedName, transName, (basicClass == null ? 0 : basicClass.length));
            }
            LogWrapper.LOGGER.warn("Ending transform of {{} ({})} Start Length: {}", name, transformedName, (basicClass == null ? 0 : basicClass.length));
        } else {
            for (final IClassTransformer transformer : transformers) {
                basicClass = transformer.transform(name, transformedName, basicClass);
            }
        }
        return basicClass;
    }

    @Override
    public void addURL(final URL url) {
        if (sources.add(url)) {
            super.addURL(url);
        }
    }

    public LinkedHashSet<URL> getSources() {
        return sources;
    }

    private byte[] readFully(InputStream stream) {
        try {
            byte[] buffer = getOrCreateBuffer();

            int read;
            int totalLength = 0;
            while ((read = stream.read(buffer, totalLength, buffer.length - totalLength)) != -1) {
                totalLength += read;

                // Extend our buffer
                if (totalLength >= buffer.length - 1) {
                    byte[] newBuffer = new byte[buffer.length + BUFFER_SIZE];
                    System.arraycopy(buffer, 0, newBuffer, 0, buffer.length);
                    buffer = newBuffer;
                }
            }

            final byte[] result = new byte[totalLength];
            System.arraycopy(buffer, 0, result, 0, totalLength);
            return result;
        } catch (Throwable t) {
            LogWrapper.LOGGER.error("Problem loading class", t);
            return new byte[0];
        }
    }

    private byte[] getOrCreateBuffer() {
        byte[] buffer = loadBuffer.get();
        if (buffer == null) {
            loadBuffer.set(new byte[BUFFER_SIZE]);
            buffer = loadBuffer.get();
        }
        return buffer;
    }

    public List<IClassTransformer> getTransformers() {
        return Collections.unmodifiableList(transformers);
    }

    public void addClassLoaderExclusion(String toExclude) {
        classLoaderExceptions.add(toExclude);
    }

    public void addTransformerExclusion(String toExclude) {
        transformerExceptions.add(toExclude);
    }

    public byte[] getClassBytes(String name) throws IOException {
        if (negativeResourceCache.contains(name)) {
            return null;
        } else if (resourceCache.containsKey(name)) {
            return resourceCache.get(name);
        }
        if (name.indexOf('.') == -1) {
            for (final String reservedName : RESERVED_NAMES) {
                if (name.toUpperCase(Locale.ENGLISH).startsWith(reservedName)) {
                    final byte[] data = getClassBytes("_" + name);
                    if (data != null) {
                        resourceCache.put(name, data);
                        return data;
                    }
                }
            }
        }

        InputStream classStream = null;
        try {
            final String resourcePath = name.replace('.', '/').concat(".class");
            URL classResource = findResource0(resourcePath);

            if (classResource == null) {
                if (DEBUG)
                    LogWrapper.LOGGER.warn("Failed to find class resource {}", resourcePath);
                negativeResourceCache.add(name);
                return null;
            }
            classStream = classResource.openStream();

            if (DEBUG)
                LogWrapper.LOGGER.warn("Loading class {} from resource {}", name, classResource);
            final byte[] data = readFully(classStream);
            resourceCache.put(name, data);
            return data;
        } finally {
            closeSilently(classStream);
        }
    }

    private URL findResource0(String name) {
        URL resource = super.findResource(name);
        if (resource != null) {
            return resource;
        }
        if (GET_SYSTEM_RESOURCE != null) {
            try {
                return (URL) GET_SYSTEM_RESOURCE.invoke(name);
            } catch (Throwable e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    private static void closeSilently(Closeable closeable) {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (IOException ignored) {
            }
        }
    }

    @SuppressWarnings("unused")
    public void clearNegativeEntries(Set<String> entriesToClear) {
        negativeResourceCache.removeAll(entriesToClear);
    }
}
