/*
 * Copyright 2014 Goblom.
 *
 * All Rights Reserved unless otherwise explicitly stated.
 */

package org.goblom.injector.events;

import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.goblom.injector.inject.Configable;
import org.goblom.injector.inject.Informable;
import org.goblom.injector.inject.Injectable;
import org.goblom.injector.inject.Unloadable;

/**
 *
 * @author Goblom
 */
public class InjectEvent extends Event implements Cancellable {

    private static final HandlerList handlers = new HandlerList();
    private boolean cancelled = false;
    private final Injectable injected;
    
    @Override
    public HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }
    
    public InjectEvent(Injectable inject) {
        this.injected = inject;
    }
    
    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean bln) {
        this.cancelled = bln;
    }
    
    public Injectable getInjected() {
        return injected;
    }
    
    public boolean isInformable() {
        return injected instanceof Informable;
    }
    
    public boolean isUnloadable() {
        return injected instanceof Unloadable;
    }
    
    public boolean isConfigable() {
        return injected instanceof Configable;
    }
}
