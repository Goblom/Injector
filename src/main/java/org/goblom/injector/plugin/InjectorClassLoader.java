/*
 * Copyright 2014 Goblom.
 *
 * All Rights Reserved unless otherwise explicitly stated.
 */

package org.goblom.injector.plugin;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.apache.commons.lang.Validate;
import org.bukkit.plugin.InvalidPluginException;
import org.goblom.injector.inject.InjectablePlugin;

/**
 *
 * @author Goblom
 * @deprecated still in use but deprecated until further notice
 */
@Deprecated
public final class InjectorClassLoader extends URLClassLoader {
    
    private final InjectorPluginLoader loader;
    private final Map<String, Class<?>> classes = new HashMap();
    private final File file;
    final InjectablePlugin plugin;
    private InjectablePlugin pluginInit;
    private IllegalStateException pluginState;
    
    InjectorClassLoader(final InjectorPluginLoader loader, final ClassLoader parent, File file) throws InvalidPluginException, MalformedURLException {
        super(new URL[] { file.toURI().toURL() }, parent);
                
        Validate.notNull(loader, "Loader cannot be null");
        
        this.loader = loader;
        this.file = file;
        
        if (!file.getName().endsWith(".class")) {
            throw new IllegalArgumentException("File is not a class file!");
        }
        
        try {
            String name = file.getName().substring(0, file.getName().indexOf("."));
            
            Class clazz = Class.forName(name, true, this);
            Object o = clazz.newInstance();
            
            if (o instanceof InjectablePlugin) {
                this.plugin = (InjectablePlugin) o;
            } else {
                throw new InvalidPluginException("THERE WAS A GOD DAMN ERROR WITH THE INSTANCE");
            }
        } catch (IllegalAccessException ex) {
            throw new InvalidPluginException("No public constructor", ex);
        } catch (InstantiationException ex) {
            throw new InvalidPluginException("Abnormal plugin type", ex);
        } catch (ClassNotFoundException ex) {
            throw new InvalidPluginException("Plugin Class not found", ex);
        }
    }
    
    Set<String> getClasses() {
        return classes.keySet();
    }
    
    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        return findClass(name, true);
    }
    
    Class<?> findClass(String name, boolean checkGlobal) throws ClassNotFoundException {
        if (name.startsWith("org.bukkit.") || name.startsWith("net.minecraft.") || name.startsWith("org.goblom.injector.")) {
            throw new ClassNotFoundException(name);
        }
        Class<?> result = classes.get(name);

        if (result == null) {
            if (checkGlobal) {
                result = loader.getClassByName(name);
            }

            if (result == null) {
                result = super.findClass(name);

                if (result != null) {
                    loader.setClass(name, result);
                }
            }

            classes.put(name, result);
        }

        return result;
    }
    
    /**
     * @deprecated not meant to be called publicly
     */
    @Deprecated
    public synchronized void initialize(InjectablePlugin iPlugin) {
        Validate.notNull(iPlugin, "Initializing plugin cannot be null");
        
        if (this.plugin != null && this.pluginInit != null) {
            throw new IllegalArgumentException("InjectablePlugin already initialized");
        }
        
        this.pluginState = new IllegalStateException("Initial initialization");
        this.pluginInit = iPlugin;
        
        iPlugin.init(this, loader);
    }
}
