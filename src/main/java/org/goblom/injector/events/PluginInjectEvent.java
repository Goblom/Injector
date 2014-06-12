/*
 * Copyright 2014 Goblom.
 *
 * All Rights Reserved unless otherwise explicitly stated.
 */

package org.goblom.injector.events;

import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.goblom.injector.inject.InjectablePlugin;

/**
 *
 * @author Goblom
 */
public class PluginInjectEvent extends Event implements Cancellable {

    public static final HandlerList handlers = new HandlerList();
    private boolean cancelled = false;
    private final InjectablePlugin plugin;
    
    @Override
    public HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }
    
    public PluginInjectEvent(InjectablePlugin plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean bln) {
        this.cancelled = bln;
    }
    
    public InjectablePlugin getInjectedPlugin() {
        return plugin;
    }
}
