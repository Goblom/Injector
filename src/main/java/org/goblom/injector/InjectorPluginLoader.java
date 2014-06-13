/*
 * Copyright 2014 Goblom.
 *
 * All Rights Reserved unless otherwise explicitly stated.
 */

package org.goblom.injector;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.regex.Pattern;
import org.apache.commons.lang.Validate;
import org.bukkit.Bukkit;
import org.bukkit.Warning;
import org.bukkit.Warning.WarningState;
import org.bukkit.event.Event;
import org.bukkit.event.EventException;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.event.server.PluginEnableEvent;
import org.bukkit.plugin.AuthorNagException;
import org.bukkit.plugin.EventExecutor;
import org.bukkit.plugin.InvalidDescriptionException;
import org.bukkit.plugin.InvalidPluginException;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.PluginLoader;
import org.bukkit.plugin.RegisteredListener;
import org.bukkit.plugin.TimedRegisteredListener;
import org.bukkit.plugin.UnknownDependencyException;
import org.bukkit.plugin.java.JavaPluginLoader;
import org.goblom.injector.inject.InjectablePlugin;

/**
 *
 * @author Goblom
 */
public class InjectorPluginLoader implements PluginLoader {

    private final InjectorPlugin plugin;

    private final JavaPluginLoader jpl;
    private Map<String, ClassLoader> loaders = new LinkedHashMap();
    
    public InjectorPluginLoader(InjectorPlugin plugin) {
        this.plugin = plugin;
        this.jpl = new JavaPluginLoader(Bukkit.getServer());
    }
    
    public JavaPluginLoader getJavaPluginLoader() {
        return jpl;
    }
    
    @Deprecated
    @Override
    public Plugin loadPlugin(File file) throws InvalidPluginException, UnknownDependencyException {
        return jpl.loadPlugin(file);
    }

    @Deprecated
    @Override
    public PluginDescriptionFile getPluginDescription(File file) throws InvalidDescriptionException {
        return jpl.getPluginDescription(file);
    }

    @Deprecated
    @Override
    public Pattern[] getPluginFileFilters() {
        return jpl.getPluginFileFilters();
    }

    @Override
    public Map<Class<? extends Event>, Set<RegisteredListener>> createRegisteredListeners(Listener listener, Plugin plugin) {
        Validate.isTrue(plugin instanceof InjectablePlugin, "Plugin is not an InjectablePlugin");
        Validate.notNull(plugin, "Plugin cannot be null");
        Validate.notNull(listener, "Listener cannot be null");
        
        boolean useTimings = Bukkit.getServer().getPluginManager().useTimings();
        Map<Class<? extends Event>, Set<RegisteredListener>> ret = new HashMap<Class<? extends Event>, Set<RegisteredListener>>();
        Set<Method> methods;
        try {
            Method[] publicMethods = listener.getClass().getMethods();
            methods = new HashSet<Method>(publicMethods.length, Float.MAX_VALUE);
            
            methods.addAll(Arrays.asList(publicMethods));
            methods.addAll(Arrays.asList(listener.getClass().getDeclaredMethods()));
            
//            for (Method method : publicMethods) {
//                methods.add(method);
//            }
//            
//            for (Method method : listener.getClass().getDeclaredMethods()) {
//                methods.add(method);
//            }
        } catch (NoClassDefFoundError e) {
            plugin.getLogger().severe("InjectablePlugin " + plugin.getDescription().getFullName() + " has failed register events for " + listener.getClass() + " because " + e.getMessage() + " does not exist.");
            return ret;
        }
        
        for (final Method method : methods) {
            final EventHandler eh = method.getAnnotation(EventHandler.class);
            
            if (eh == null) continue;
            
            final Class<?> checkClass;
            
            if (method.getParameterTypes().length != 1 || !Event.class.isAssignableFrom(checkClass = method.getParameterTypes()[0])) {
                plugin.getLogger().severe(plugin.getDescription().getFullName() + " attempted to register an invalid EventHandler method signature \"" + method.toGenericString() + "\" in " + listener.getClass());
                continue;
            }
            
            final Class<? extends Event> eventClass = checkClass.asSubclass(Event.class);
            method.setAccessible(true);
            Set<RegisteredListener> eventSet = ret.get(eventClass);
            if (eventSet == null) {
                eventSet = new HashSet<RegisteredListener>();
                ret.put(eventClass, eventSet);
            }
            
            for (Class<?> clazz = eventClass; Event.class.isAssignableFrom(clazz); clazz = clazz.getSuperclass()) {
                // This loop checks for extending deprecated events
                if (clazz.getAnnotation(Deprecated.class) != null) {
                    Warning warning = clazz.getAnnotation(Warning.class);
                    WarningState warningState = Bukkit.getWarningState();
                    if (!warningState.printFor(warning)) {
                        break;
                    }
                    plugin.getLogger().log(
                            Level.WARNING,
                            String.format(
                                    "\"%s\" has registered a listener for %s on method \"%s\", but the event is Deprecated." +
                                    " \"%s\"; please notify the authors %s.",
                                    plugin.getDescription().getFullName(),
                                    clazz.getName(),
                                    method.toGenericString(),
                                    (warning != null && warning.reason().length() != 0) ? warning.reason() : "Server performance will be affected",
                                    Arrays.toString(plugin.getDescription().getAuthors().toArray())),
                            warningState == WarningState.ON ? new AuthorNagException(null) : null);
                    break;
                }
            }
            
            EventExecutor executor = new EventExecutor() {
                @Override
                public void execute(Listener listener, Event event) throws EventException {
                    try {
                        if (eventClass.isAssignableFrom(event.getClass())) {
                            return;
                        }

                        method.invoke(listener, event);
                    } catch (InvocationTargetException ex) {
                        throw new EventException(ex.getCause());
                    } catch (Throwable t) {
                        throw new EventException(t);
                    }
                }
            };
            
            if (useTimings) {
                eventSet.add(new TimedRegisteredListener(listener, executor, eh.priority(), plugin, eh.ignoreCancelled()));
            } else {
                eventSet.add(new RegisteredListener(listener, executor, eh.priority(), plugin, eh.ignoreCancelled()));
            }
        }
        return ret;
    }

    @Override
    public void enablePlugin(Plugin plugin) {
        Validate.isTrue(plugin instanceof InjectablePlugin, "Plugin is not associated with this PluginLoader");
        
        if (!plugin.isEnabled()) {
            plugin.getLogger().info("Enabling InjectedPlugin " + plugin.getName());
            
            InjectablePlugin iPlugin = (InjectablePlugin) plugin;
            
            String pluginName = iPlugin.getName();
            
            if (!loaders.containsKey(pluginName)) {
                loaders.put(pluginName, (ClassLoader) iPlugin.getClass().getClassLoader());
            }
            
            try {
                iPlugin.setEnabled(true);
            } catch (Throwable e) {
                e.printStackTrace();
                this.plugin.getLogger().log(Level.SEVERE, "Error occurred while enabling InjectedPlugin " + plugin.getName());
            }
            
            Bukkit.getPluginManager().callEvent(new PluginEnableEvent(plugin));
        }
    }

    @Override
    public void disablePlugin(Plugin plugin) {
        Validate.isTrue(plugin instanceof InjectablePlugin, "Plugin is not associated with this PluginLoader");
        
        if (plugin.isEnabled()) {
            String message = String.format("Disabling %s", plugin.getName());
            plugin.getLogger().info(message);
            
            Bukkit.getPluginManager().callEvent(new PluginDisableEvent(plugin));
            
            InjectablePlugin iPlugin = (InjectablePlugin) plugin;
            ClassLoader cLoader = iPlugin.getClass().getClassLoader();
            
            try {
                iPlugin.setEnabled(true);
            } catch (Throwable ex) {
                this.plugin.getLogger().log(Level.SEVERE, "Error occurred while disabling " + plugin.getName());
            }
            
            loaders.remove(plugin.getName());
            
            if (cLoader instanceof ClassLoader) {
                //remove class
            }
        }
    }
    
}
