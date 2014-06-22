/*
 * Copyright 2014 Goblom.
 *
 * All Rights Reserved unless otherwise explicitly stated.
 */

package org.goblom.injector.plugin;

import java.io.File;
import java.io.FileNotFoundException;
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
import org.bukkit.Server;
import org.bukkit.Warning;
import org.bukkit.Warning.WarningState;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.configuration.serialization.ConfigurationSerialization;
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
import org.goblom.injector.Injector;
import org.goblom.injector.inject.InjectablePlugin;

/**
 *
 * @author Goblom
 */
public class InjectorPluginLoader implements PluginLoader {

    private final Server server;
    private final Pattern[] fileFilters = new Pattern[] { Pattern.compile("\\.class$"), };
    private final Map<String, InjectorClassLoader> loaders = new LinkedHashMap();
    private final Map<String, Class<?>> classes = new HashMap();
    
    public InjectorPluginLoader(Server server) {
        this.server = server;
    }
    
    @Override
    public Plugin loadPlugin(File file) throws InvalidPluginException, UnknownDependencyException {
        Validate.notNull(file, "File cannot be null");
        
        if (!file.exists()) {
            throw new InvalidPluginException(new FileNotFoundException(file.getPath() + " does not exist"));
        }
        
        final InjectorClassLoader loader;
        
        try {
            loader = new InjectorClassLoader(this, getClass().getClassLoader(), file);
            loader.initialize(loader.plugin);
            
        } catch (InvalidPluginException ex) {
            throw ex;
        } catch (Throwable ex) {
            throw new InvalidPluginException(ex);
        }
        
        Injector.getLogger().info("Successfully loaded " + loader.plugin.getName());
        loaders.put(loader.plugin.getName(), loader);
        
        return loader.plugin;
    }

    @Deprecated
    @Override
    public PluginDescriptionFile getPluginDescription(File file) throws InvalidDescriptionException {
        throw new UnsupportedOperationException("InjectorPluginLoader does not support this");
    }

    @Override
    public Pattern[] getPluginFileFilters() {
        return fileFilters;
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
                        if (!eventClass.isAssignableFrom(event.getClass())) {
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
            Injector.getLogger().info("Injecting plugin " + plugin.getName());
            
            InjectablePlugin iPlugin = (InjectablePlugin) plugin;
            
            String pluginName = iPlugin.getName();
            
            if (!loaders.containsKey(pluginName)) {
                loaders.put(pluginName, (InjectorClassLoader) iPlugin.getClass().getClassLoader());
            }
            
            try {
                iPlugin.setEnabled(true);
            } catch (Throwable e) {
                e.printStackTrace();
                Injector.getLogger().log(Level.SEVERE, "Error while injecting " + plugin.getName());
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
                Injector.getLogger().log(Level.SEVERE, "Error occurred while disabling " + plugin.getName());
            }
            
            loaders.remove(plugin.getName());
            
            if (cLoader instanceof InjectorClassLoader) {
                InjectorClassLoader loader = (InjectorClassLoader) cLoader;
                Set<String> names = loader.getClasses();
                
                for (String name : names) {
                    removeClass(name);
                }
            }
        }
    }
    
    Class<?> getClassByName(final String name) {
        Class<?> cachedClass = classes.get(name);

        if (cachedClass != null) {
            return cachedClass;
        } else {
            for (String current : loaders.keySet()) {
                InjectorClassLoader loader = loaders.get(current);

                try {
                    cachedClass = loader.findClass(name, false);
                } catch (ClassNotFoundException cnfe) {}
                if (cachedClass != null) {
                    return cachedClass;
                }
            }
        }
        return null;
    }
    
    void setClass(final String name, final Class<?> clazz) {
        if (!classes.containsKey(name)) {
            classes.put(name, clazz);

            if (ConfigurationSerializable.class.isAssignableFrom(clazz)) {
                Class<? extends ConfigurationSerializable> serializable = clazz.asSubclass(ConfigurationSerializable.class);
                ConfigurationSerialization.registerClass(serializable);
            }
        }
    }
    
    private void removeClass(String name) {
        Class<?> clazz = classes.remove(name);

        try {
            if ((clazz != null) && (ConfigurationSerializable.class.isAssignableFrom(clazz))) {
                Class<? extends ConfigurationSerializable> serializable = clazz.asSubclass(ConfigurationSerializable.class);
                ConfigurationSerialization.unregisterClass(serializable);
            }
        } catch (NullPointerException ex) {
            // Boggle!
            // (Native methods throwing NPEs is not fun when you can't stop it before-hand)
        }
    }
}
