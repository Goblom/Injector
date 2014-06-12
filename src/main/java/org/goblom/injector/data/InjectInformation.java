/*
 * Copyright 2014 Goblom.
 *
 * All Rights Reserved unless otherwise explicitly stated.
 */

package org.goblom.injector.data;

import org.goblom.injector.factory.InformationFactory;

/**
 *
 * @author Goblom
 */
public class InjectInformation {
    private String author;
    private String version;
    private String website;
    
    public String getAuthor() {
        return author;
    }
    
    public String getVersion() {
        return version;
    }
    
    public String getWebsite() {
        return website;
    }
    
    public boolean hasAuthor() {
        return (author != null) && (!author.isEmpty());
    }
    
    public boolean hasVersion() {
        return (version != null) && (!version.isEmpty());
    }
    
    public boolean hasWebsite() {
        return (website != null) && (!version.isEmpty());
    }
    
    public void setAuthor(String author) {
        this.author = author;
    }
    
    public void setVersion(String version) {
        this.version = version;
    }
    
    public void setWebsite(String website) {
        this.website = website;
    }
    
    public static InformationFactory builder() {
        return new InformationFactory(new InjectInformation());
    }
}
