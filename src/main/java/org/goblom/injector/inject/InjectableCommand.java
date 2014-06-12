/*
 * Copyright 2014 Goblom.
 *
 * All Rights Reserved unless otherwise explicitly stated.
 */

package org.goblom.injector.inject;

import java.util.List;
import java.util.logging.Logger;
import org.bukkit.command.CommandSender;
import org.goblom.injector.Injector;
import org.goblom.injector.InjectorAPI;

/**
 *
 * @author Goblom
 */
public abstract class InjectableCommand extends Configable implements Unloadable, Informable {
    
    public InjectableCommand() {}
    
    public InjectorAPI getInjector() {
        return Injector.getInjector();
    }
    
    public Logger getLogger() {
        return getInjector().getLogger();
    }
    
    @Override
    public void onInject() { }
    
    @Override
    public void onUnload() { }
    
    public abstract String getLabel();
    
    public abstract boolean execute(CommandSender sender, String commandLabel, String[] args);
    
    public String getDescription() {
        return null;
    }
    
    public List<String> getAliases() {
        return null;
    }
    
    public String getUsage() {
        return null;
    }
    
    public String getPermission() {
        return null;
    }
    
    public String getPermissionMessage() {
        return null;
    }
}
