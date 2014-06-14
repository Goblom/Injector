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
public interface InjectorAPI {
    
    public File getDataFolder();
    
    public Logger getLogger();
        
    public boolean canInjectCommands();
    
    public List<Injectable> getInjected();
    
    public boolean inject(Class<? extends Injectable> inject);
    
    public boolean unload(Class<? extends Unloadable> clazz);
    
    public <T extends Injectable> T getInjectable(String name);
    
    public InjectorPluginLoader getInjectablePluginLoader();
    
    public Plugin getBukkit();
}
