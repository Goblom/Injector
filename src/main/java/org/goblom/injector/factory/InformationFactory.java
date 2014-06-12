/*
 * Copyright 2014 Goblom.
 *
 * All Rights Reserved unless otherwise explicitly stated.
 */

package org.goblom.injector.factory;

import org.goblom.injector.data.InjectInformation;

/**
 *
 * @author Goblom
 */
public class InformationFactory {
    private final InjectInformation info;
    
    public InformationFactory(InjectInformation info) {
        this.info = info;
    }
    
    public InformationFactory withAuthor(String author) {
        info.setAuthor(author);
        return this;
    }
    
    public InformationFactory withVersion(String version) {
        info.setVersion(version);
        return this;
    }
    
    public InformationFactory withWebsite(String website) {
        info.setWebsite(website);
        return this;
    }
    
    public InjectInformation build() {
        return info;
    }
}
