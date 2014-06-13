/*
 * Copyright 2014 Goblom.
 *
 * All Rights Reserved unless otherwise explicitly stated.
 */

package org.goblom.injector;

import java.io.File;
import java.util.List;
import java.util.logging.Logger;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginLoader;
import org.goblom.injector.inject.Injectable;
import org.goblom.injector.inject.Unloadable;

/**
 *
 * @author Goblom
 */
public class Injector {
    
    private static InjectorAPI api;
    
    protected static void setInjector(InjectorAPI api) {
        if (Injector.api == null) {
            Injector.api = api;
        }
    }
    
    public static InjectorAPI getInjector() {
        return api;
    }
    
    public static File getDataFolder() {
        return api.getDataFolder();
    }
    
    public static Logger getLogger() {
        return api.getLogger();
    }
    
    public static List<Injectable> getInjected() {
        return api.getInjected();
    }
    
    public static PluginLoader getInjectablePluginLoader() {
        return api.getInjectablePluginLoader();
    }
    
    public static boolean canInjectCommands() {
        return api.canInjectCommands();
    }
    
    public static boolean inject(Class<? extends Injectable> inject) {
        return api.inject(inject);
    }
    
    public static boolean unload(Class<? extends Unloadable> clazz) {
        return api.unload(clazz);
    }
    
    public static <T extends Injectable> T getInjectable(String name) {
        return api.getInjectable(name);
    }
    
    public static Plugin getBukkit() {
        return api.getBukkit();
    }
}
