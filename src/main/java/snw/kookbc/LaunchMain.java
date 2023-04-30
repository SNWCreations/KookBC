/*
 * License: https://github.com/Mojang/LegacyLauncher
 */
package snw.kookbc;

import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;
import snw.kookbc.impl.launch.ITweaker;
import snw.kookbc.impl.launch.LaunchClassLoader;
import snw.kookbc.impl.launch.LogWrapper;
import snw.kookbc.impl.plugin.PluginClassLoaderDelegate;

import java.io.File;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;

// Invoke the main method under this class to launch KookBC with Mixin support.
// author: huanmeng_qwq
// added since 2023/1/24
// WARNING: Do not use this class in embedded environments!
public class LaunchMain {
    private static final String MIXIN_TWEAK = "snw.kookbc.impl.mixin.MixinTweaker";
    private static final String DEFAULT_TWEAK = "snw.kookbc.impl.launch.LaunchMainTweaker";
    public static Map<String, Object> blackboard;

    // We won't use Main#MAIN_THREAD_NAME
    // The Main class should not be loaded at this time
    private static final String MAIN_THREAD_NAME = "Main Thread";

    public static void main(String[] args) {
        LaunchMain launch = new LaunchMain();
        System.setProperty("kookbc.launch", "true");
        List<String> argsList = new ArrayList<>(Arrays.asList(args));
        if (argsList.stream().noneMatch(e -> e.contains("--tweakClass"))) {
            argsList.add("--tweakClass");
            argsList.add(MIXIN_TWEAK);
            argsList.add("--tweakClass");
            argsList.add(DEFAULT_TWEAK);
        }
        args = argsList.toArray(new String[0]);
        Thread.currentThread().setName(MAIN_THREAD_NAME);
        // Actually here should not be warning, but the logging level of the LaunchWrapper is WARN.
        // Also, I think this message can let the user know they are running KookBC under Launch mode.
        LogWrapper.LOGGER.warn("Launching KookBC with Mixin support");
        LogWrapper.LOGGER.warn("The author of Mixin support: huanmeng_qwq@Github"); // thank you!  --- SNWCreations
        LogWrapper.LOGGER.warn("Tips: You can safely ignore this.");
        LogWrapper.LOGGER.warn("But if you're really sure you don't need Mixin support, visit the following link:");
        LogWrapper.LOGGER.warn("https://github.com/SNWCreations/KookBC/blob/main/docs/KookBC_CommandLine.md");
        LogWrapper.LOGGER.warn("The documentation will tell you how can you launch KookBC without Mixin support.");
        launch.launch(args);
    }

    public static LaunchClassLoader classLoader;

    private LaunchMain() {
        // Get classpath
        List<URL> urls = new ArrayList<>();
        if (getClass().getClassLoader() instanceof URLClassLoader) {
            Collections.addAll(urls, ((URLClassLoader) getClass().getClassLoader()).getURLs());
        } else {
            for (String s : System.getProperty("java.class.path").split(File.pathSeparator)) {
                try {
                    urls.add(new File(s).toURI().toURL());
                } catch (MalformedURLException e) {
                    e.printStackTrace();
                }
            }
        }
        classLoader = new LaunchClassLoader(urls.toArray(new URL[0]));
        blackboard = new HashMap<>();
    }

    private void launch(String[] args) {
        final OptionParser parser = new OptionParser();
        parser.allowsUnrecognizedOptions();

        final OptionSpec<String> tweakClassOption = parser.accepts("tweakClass", "Tweak class(es) to load").withRequiredArg().defaultsTo(MIXIN_TWEAK, DEFAULT_TWEAK);
        final OptionSpec<String> nonOption = parser.nonOptions();

        final OptionSet options = parser.parse(args);
        final List<String> tweakClassNames = new ArrayList<>(options.valuesOf(tweakClassOption));

        final List<String> argumentList = new ArrayList<>();
        // This list of names will be interacted with through tweakers. They can append to this list
        // any 'discovered' tweakers from their preferred mod loading mechanism
        // By making this object discoverable and accessible it's possible to perform
        // things like cascading of tweakers
        blackboard.put("TweakClasses", tweakClassNames);

        // This argument list will be constructed from all tweakers. It is visible here so
        // all tweakers can figure out if a particular argument is present, and add it if not
        blackboard.put("ArgumentList", argumentList);

        // This is to prevent duplicates - in case a tweaker decides to add itself or something
        final Set<String> allTweakerNames = new HashSet<>();
        // The 'definitive' list of tweakers
        final List<ITweaker> allTweakers = new ArrayList<>();
        try {
            final List<ITweaker> tweakers = new ArrayList<>(tweakClassNames.size() + 1);
            // The list of tweak instances - may be useful for interoperability
            blackboard.put("Tweaks", tweakers);
            // The primary tweaker (the first one specified on the command line) will actually
            // be responsible for providing the 'main' name and generally gets called first
            ITweaker primaryTweaker = null;
            // This loop will terminate, unless there is some sort of pathological tweaker
            // that reinserts itself with a new identity every pass
            // It is here to allow tweakers to "push" new tweak classes onto the 'stack' of
            // tweakers to evaluate allowing for cascaded discovery and injection of tweakers
            do {
                for (final Iterator<String> it = tweakClassNames.iterator(); it.hasNext(); ) {
                    final String tweakName = it.next();
                    // Safety check - don't reprocess something we've already visited
                    if (allTweakerNames.contains(tweakName)) {
                        LogWrapper.LOGGER.warn("Tweak class name {} has already been visited -- skipping", tweakName);
                        // remove the tweaker from the stack otherwise it will create an infinite loop
                        it.remove();
                        continue;
                    } else {
                        allTweakerNames.add(tweakName);
                    }
                    LogWrapper.LOGGER.info("Loading tweak class name {}", tweakName);

                    // Ensure we allow the tweak class to load with the parent classloader
                    classLoader.addClassLoaderExclusion(tweakName.substring(0, tweakName.lastIndexOf('.')));
                    final ITweaker tweaker = (ITweaker) Class.forName(tweakName, true, classLoader).getConstructor().newInstance();
                    tweakers.add(tweaker);

                    // Remove the tweaker from the list of tweaker names we've processed this pass
                    it.remove();
                    // If we haven't visited a tweaker yet, the first will become the 'primary' tweaker
                    if (primaryTweaker == null) {
                        LogWrapper.LOGGER.info("Using primary tweak class name {}", tweakName);
                        primaryTweaker = tweaker;
                    }
                }

                // Now, iterate all the tweakers we just instantiated
                for (final Iterator<ITweaker> it = tweakers.iterator(); it.hasNext(); ) {
                    final ITweaker tweaker = it.next();
                    LogWrapper.LOGGER.info("Calling tweak class {}", tweaker.getClass().getName());
                    tweaker.acceptOptions(options.valuesOf(nonOption));
                    tweaker.injectIntoClassLoader(classLoader);
                    allTweakers.add(tweaker);
                    // again, remove from the list once we've processed it, so we don't get duplicates
                    it.remove();
                }
                // continue around the loop until there's no tweak classes
            } while (!tweakClassNames.isEmpty());

            // Once we're done, we then ask all the tweakers for their arguments and add them all to the
            // master argument list
            for (final ITweaker tweaker : allTweakers) {
                String[] launchArguments = tweaker.getLaunchArguments();
                if (launchArguments != null) {
                    argumentList.addAll(Arrays.asList(launchArguments));
                }
            }

            if (primaryTweaker == null) {
                throw new NullPointerException("Tweaker not found");
            }

            // Finally, we turn to the primary tweaker, and let it tell us where to go to launch

            for (ITweaker allTweaker : allTweakers) {
                final String launchTarget = allTweaker.getLaunchTarget();
                if (launchTarget != null && !launchTarget.isEmpty()) {
                    final Class<?> clazz = Class.forName(launchTarget, false, classLoader);
                    final Method mainMethod = clazz.getMethod("main", String[].class);
                    Thread main = new Thread(() -> {
                        try {
                            mainMethod.invoke(null, (Object) argumentList.toArray(new String[0]));
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }, clazz.getSimpleName());
                    main.setContextClassLoader(PluginClassLoaderDelegate.INSTANCE);
                    main.start();
                }
            }
        } catch (Exception e) {
            LogWrapper.LOGGER.error("Unable to launch", e);
            System.exit(1);
        }
    }
}
