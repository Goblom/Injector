/*
 * Copyright 2014 Goblom.
 *
 * All Rights Reserved unless otherwise explicitly stated.
 */

package org.goblom.injector.factory;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;
import org.bukkit.plugin.PluginDescriptionFile;
import org.goblom.injector.inject.InjectablePlugin;

/**
 *
 * @author Goblom
 */
public class PluginDescriptionFactory {
    
    private final InjectablePlugin plugin;
    private String name;
    private String version;
    private String description;
    private List<String> depend;
    private List<String> softDepend;
    private List<String> authors;
    private boolean database;
    private String website;
    
    public PluginDescriptionFactory(InjectablePlugin plugin) {
        this.plugin = plugin;
    }
    
    public PluginDescriptionFactory withName(String name) {
        this.name = name;
        return this;
    }
    
    public PluginDescriptionFactory withVersion(String version) {
        this.version = version;
        return this;
    }
    
    public PluginDescriptionFactory withDescription(String description) {
        this.description = description;
        return this;
    }
    
    public PluginDescriptionFactory withDatabase(boolean bln) {
        this.database = bln;
        return this;
    }
    
    public PluginDescriptionFactory withDepends(String... depends) {
        this.depend = Arrays.asList(depends);
        return this;
    }
    
    public PluginDescriptionFactory withSoftDepends(String... depends) {
        this.softDepend = Arrays.asList(depends);
        return this;
    }
    
    public PluginDescriptionFactory withWebsite(String website) {
        this.website = website;
        return this;
    }
    
    public PluginDescriptionFactory withAuthors(String... authors) {
        this.authors = Arrays.asList(authors);
        return this;
    }
    
    public PluginDescriptionFile build() throws Exception {
        PluginDescriptionFile pdf = new PluginDescriptionFile(name, version, plugin.getClass().getName());
        
        Field depend = pdf.getClass().getDeclaredField("depend");
        Field description = pdf.getClass().getDeclaredField("description");
        Field softDepend = pdf.getClass().getDeclaredField("softDepend");
        Field database = pdf.getClass().getDeclaredField("database");
        Field website = pdf.getClass().getDeclaredField("website");
        Field authors = pdf.getClass().getDeclaredField("authors");
        
        depend.setAccessible(true);
        description.setAccessible(true);
        softDepend.setAccessible(true);
        database.setAccessible(true);
        website.setAccessible(true);
        authors.setAccessible(true);
        
        if (this.depend != null) {
            depend.set(pdf, this.depend);
        }
        
        if (this.softDepend != null) {
            softDepend.set(pdf, this.softDepend);
        }
        
        if (this.database) {
            database.set(pdf, this.database);
        }
        
        if (this.website != null) {
            website.set(pdf, this.website);
        }
        
        if (this.authors != null) {
            authors.set(pdf, this.authors);
        }
        
        if (this.description != null) {
            description.set(pdf, this.description);
        }
        
        return pdf;
    }
    
    public static PluginDescriptionFactory of(InjectablePlugin plugin) {
        return new PluginDescriptionFactory(plugin);
    }
}
