/*
 * Copyright 2014 Goblom.
 *
 * All Rights Reserved unless otherwise explicitly stated.
 */

package org.goblom.injector;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.regex.Pattern;
import org.apache.commons.lang.Validate;
import org.bukkit.Bukkit;
import org.bukkit.event.Event;
import org.bukkit.event.Listener;
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.event.server.PluginEnableEvent;
import org.bukkit.plugin.InvalidDescriptionException;
import org.bukkit.plugin.InvalidPluginException;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.PluginLoader;
import org.bukkit.plugin.RegisteredListener;
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
    
    @Override
    public Plugin loadPlugin(File file) throws InvalidPluginException, UnknownDependencyException {
        return jpl.loadPlugin(file);
    }

    @Override
    public PluginDescriptionFile getPluginDescription(File file) throws InvalidDescriptionException {
        return jpl.getPluginDescription(file);
    }

    @Override
    public Pattern[] getPluginFileFilters() {
        return jpl.getPluginFileFilters();
    }

    @Override
    public Map<Class<? extends Event>, Set<RegisteredListener>> createRegisteredListeners(Listener ll, Plugin plugin) {
        return jpl.createRegisteredListeners(ll, plugin);
    }

    @Override
    public void enablePlugin(Plugin plugin) {
        Validate.isTrue(plugin instanceof InjectablePlugin, "Plugin is not associated with this PluginLoader");
        
        if (!plugin.isEnabled()) {
            plugin.getLogger().info("Enabling " + plugin.getName());
            
            InjectablePlugin iPlugin = (InjectablePlugin) plugin;
            
            String pluginName = iPlugin.getName();
            
            if (!loaders.containsKey(pluginName)) {
                loaders.put(pluginName, (ClassLoader) iPlugin.getClass().getClassLoader());
            }
            
            try {
                iPlugin.setEnabled(true);
            } catch (Throwable e) {
                e.printStackTrace();
                this.plugin.getLogger().log(Level.SEVERE, "Error occurred while enabling " + plugin.getName());
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
