/*
 * Copyright 2014 Goblom.
 *
 * All Rights Reserved unless otherwise explicitly stated.
 */

package org.goblom.injector.events;

import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.goblom.injector.inject.InjectableCommand;

/**
 *
 * @author Goblom
 */
public class CommandInjectEvent extends Event implements Cancellable {
    
    private final static HandlerList handlers = new HandlerList();
    private boolean cancelled = false;
    private InjectableCommand command;
    
    @Override
    public HandlerList getHandlers() {
        return handlers;
    }
    
    public static HandlerList getHandlerList() {
        return handlers;
    }
    
    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean bln) {
        this.cancelled = bln;
    }
    
    public CommandInjectEvent(InjectableCommand command) {
        this.command = command;
    }
    
    public InjectableCommand getInjectedCommand() {
        return command;
    }
}
