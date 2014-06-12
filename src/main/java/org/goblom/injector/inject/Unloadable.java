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
public interface Unloadable {
    
    public String getName();
    
    public void onUnload();
}
