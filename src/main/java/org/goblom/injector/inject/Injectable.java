/*
 * Copyright 2014 Goblom.
 *
 * All Rights Reserved unless otherwise explicitly stated.
 */

package org.goblom.injector.inject;

/**
 *
 * @author Goblom
 */
public abstract class Injectable {
    private boolean isInjected = false;
    
    public Injectable() {}
    
    public abstract void onInject();
    
    public final void setInjected(boolean injected) {
        if (isInjected != injected) {
            this.isInjected = injected;
            
            if (isInjected()) {
                onInject();
            } else if (this instanceof Unloadable) {
                ((Unloadable) this).onUnload();
            }
        }        
    }
    
    public final boolean isInjected() {
        return isInjected;
    }
    
    public String getName() {
        return getClass().getName();
    }
}
