//
// MIT License
//
// Copyright (c) 2022 Alexander Söderberg & Contributors
//
// Permission is hereby granted, free of charge, to any person obtaining a copy
// of this software and associated documentation files (the "Software"), to deal
// in the Software without restriction, including without limitation the rights
// to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
// copies of the Software, and to permit persons to whom the Software is
// furnished to do so, subject to the following conditions:
//
// The above copyright notice and this permission notice shall be included in all
// copies or substantial portions of the Software.
//
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
// IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
// FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
// AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
// LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
// OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
// SOFTWARE.
//
package snw.kookbc.impl.launch;

import net.bytebuddy.agent.ByteBuddyAgent;
import org.jetbrains.annotations.NotNull;
import snw.jkook.plugin.MarkedClassLoader;

import java.lang.instrument.Instrumentation;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.LinkedHashSet;
import java.util.function.Consumer;
import java.util.jar.JarFile;

/**
 * @author huanmeng_qwq
 */
public interface AccessClassLoader extends MarkedClassLoader {
    static AccessClassLoader of(ClassLoader classLoader) {
        if (classLoader instanceof AccessClassLoader) {
            return (AccessClassLoader) classLoader;
        } else if (Reflection.isSupported() && classLoader instanceof URLClassLoader) {
            Reflection reflection = new Reflection((URLClassLoader) classLoader);
            return new AccessURLClassLoader(classLoader, reflection::addURL);
        } else if (ClassPathAgent.instrumentation != null) {
            return new AccessURLClassLoader(classLoader, url -> {
                try {
                    if (url.getProtocol().equals("file")) {
                        ClassPathAgent.instrumentation.appendToSystemClassLoaderSearch(new JarFile(url.getFile()));
                    } else {
                        // !!!!Error!!!!
                        ClassPathAgent.instrumentation.appendToSystemClassLoaderSearch(new JarFile(url.getFile(), false));
                    }
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
        }
        return new AccessClassLoaderImpl(classLoader);
    }

    void addURL(URL url);

    Class<?> findClass(String name) throws ClassNotFoundException;

    Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException;

    class AccessClassLoaderImpl extends URLClassLoader implements AccessClassLoader {
        protected final LinkedHashSet<URL> sources = new LinkedHashSet<>();

        public AccessClassLoaderImpl(ClassLoader classLoader) {
            super(new URL[0], classLoader);
        }

        @Override
        public void addURL(URL url) {
            if (sources.add(url)) {
                super.addURL(url);
            }
        }

        @Override
        public Class<?> findClass(String name) throws ClassNotFoundException {
            return super.findClass(name);
        }

        @Override
        public Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
            return super.loadClass(name, resolve);
        }
    }

    class AccessURLClassLoader extends AccessClassLoaderImpl {
        private final Consumer<URL> addUrlFunc;

        public AccessURLClassLoader(ClassLoader loader, Consumer<URL> addUrlFunc) {
            super(loader);
            this.addUrlFunc = addUrlFunc;
        }

        @Override
        public void addURL(URL url) {
            if (sources.add(url)) {
                addUrlFunc.accept(url);
            }
        }
    }

    class Reflection {
        private static final Method ADD_URL_METHOD;

        static {
            Method addUrlMethod;
            try {
                addUrlMethod = URLClassLoader.class.getDeclaredMethod("addURL", URL.class);
                addUrlMethod.setAccessible(true);
            } catch (Exception e) {
                addUrlMethod = null;
            }
            ADD_URL_METHOD = addUrlMethod;
        }

        private static boolean isSupported() {
            return ADD_URL_METHOD != null;
        }

        private final URLClassLoader classLoader;

        Reflection(URLClassLoader classLoader) {
            this.classLoader = classLoader;
        }

        public void addURL(@NotNull URL url) {
            try {
                ADD_URL_METHOD.invoke(classLoader, url);
            } catch (ReflectiveOperationException e) {
                throw new RuntimeException(e);
            }
        }
    }

    class ClassPathAgent {
        protected static Instrumentation instrumentation = ByteBuddyAgent.install();

        public static void agentmain(String args, Instrumentation instrumentation) {
            if (ClassPathAgent.instrumentation == null) {
                ClassPathAgent.instrumentation = instrumentation;
            }
        }
    }
}
