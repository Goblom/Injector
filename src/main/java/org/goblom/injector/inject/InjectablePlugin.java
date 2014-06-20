/*
 * Copyright 2014 Goblom.
 *
 * All Rights Reserved unless otherwise explicitly stated.
 */

package org.goblom.injector.inject;

import com.avaje.ebean.EbeanServer;
import com.avaje.ebean.EbeanServerFactory;
import com.avaje.ebean.config.DataSourceConfig;
import com.avaje.ebean.config.ServerConfig;
import com.avaje.ebeaninternal.api.SpiEbeanServer;
import com.avaje.ebeaninternal.server.ddl.DdlGenerator;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.generator.ChunkGenerator;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.PluginLoader;
import org.goblom.injector.Injector;
import org.goblom.injector.InjectorAPI;
import org.goblom.injector.factory.PluginDescriptionFactory;
import org.goblom.injector.plugin.InjectorClassLoader;
import org.goblom.injector.plugin.InjectorPluginLoader;

/**
 *
 * @author Goblom
 */
public abstract class InjectablePlugin extends Configable implements Plugin {
    
    private InjectorPluginLoader pluginLoader;
    private InjectorClassLoader classLoader;
    
    private boolean naggable, enabled;
    
    private EbeanServer ebean;
    private PluginDescriptionFile description;
    
    //TODO
    public InjectablePlugin() { 
//        final ClassLoader classLoader = getClass().getClassLoader();
//        
//        if (!(classLoader instanceof InjectorClassLoader)) {
//            throw new IllegalStateException("InjectablePlugin requires " + InjectorClassLoader.class.getName());
//        }
//        
//        ((InjectorClassLoader) classLoader).initialize(this);
    }
    
    public ClassLoader getClassLoader() {
        return classLoader;
    }
    
    @Override
    public void onInject() { }

    public List<Class<?>> getDatabaseClasses() {
        return new ArrayList<Class<?>>();
    }
    
    private String replaceDatabaseString(String input) {
        input = input.replaceAll("\\{DIR\\}", getDataFolder().getPath().replaceAll("\\\\", "/") + "/");
        input = input.replaceAll("\\{NAME\\}", getName().replaceAll("[^\\w_-]", ""));
        return input;
    }

    public abstract PluginDescriptionFactory getDescriptionFactory();
    
    @Override
    public final PluginDescriptionFile getDescription() {
        if (description != null) {
            return description;
        }
        
        try {
            return description = getDescriptionFactory().build();
        } catch (Exception e) { 
            e.printStackTrace(); 
        }
        
        return description = new PluginDescriptionFile(getClass().getName(), "Invalid Version", getClass().getName());
    }

    @Override
    @Deprecated
    public InputStream getResource(String string) {
        throw new UnsupportedOperationException("Not supported.");
    }

    @Override
    public void saveDefaultConfig() {
        createConfig();
    }

    @Override
    @Deprecated
    public void saveResource(String string, boolean bln) { }

    @Override
    public final PluginLoader getPluginLoader() {
        return pluginLoader;
    }
    
    public final InjectorAPI getInjector() {
        return Injector.getInjector();
    }
    
    @Override
    public final Server getServer() {
        return Bukkit.getServer();
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean bln) {
        if (enabled != bln) {
            enabled = bln;
            
            if (isEnabled()) {
                onEnable();
            } else {
                onDisable();
            }
        }
    }
    
    @Override
    public void onDisable() { }

    @Override
    public void onLoad() { }

    @Override
    public void onEnable() { }

    @Override
    public boolean isNaggable() {
        return naggable;
    }

    @Override
    public void setNaggable(boolean bln) {
        this.naggable = bln;
    }

    @Override
    public EbeanServer getDatabase() {
        return ebean;
    }

    @Deprecated
    @Override
    public ChunkGenerator getDefaultWorldGenerator(String string, String string1) {
        return null;
    }

    @Override
    public Logger getLogger() {
        return getInjector().getLogger();
    }

    @Override
    @Deprecated
    public List<String> onTabComplete(CommandSender cs, Command cmnd, String string, String[] strings) {
        return null;
    }

    @Override
    @Deprecated
    public boolean onCommand(CommandSender cs, Command cmnd, String string, String[] strings) {
        return false;
    }
    
    @Override
    public final int hashCode() {
        return getName().hashCode();
    }
    
    @Override
    public final boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        
        if (obj == null) {
            return false;
        }
        
        if (!(obj instanceof InjectablePlugin)) {
            return false;
        }
        
        return getName().equals(((InjectablePlugin) obj).getName());
    }
    
    @Override
    public final String getName() {
        return getDescription().getName();
    }
    
    protected void installDDL() {
        SpiEbeanServer serv = (SpiEbeanServer) getDatabase();
        DdlGenerator gen = serv.getDdlGenerator();
        
        gen.runScript(false, gen.generateCreateDdl());
    }
    
    protected void removeDDL() {
        SpiEbeanServer serv = (SpiEbeanServer) getDatabase();
        DdlGenerator gen = serv.getDdlGenerator();

        gen.runScript(true, gen.generateDropDdl());
    }
    
    public void init(InjectorClassLoader classLoader, InjectorPluginLoader loader) {
        if (this.classLoader != null && this.pluginLoader != null || isEnabled()) {
            return;
        }
        
        this.classLoader = classLoader;
        this.pluginLoader = loader;
        
        if (getDescription().isDatabaseEnabled()) {
            ServerConfig db = new ServerConfig();
            
            db.setDefaultServer(false);
            db.setRegister(false);
            db.setClasses(getDatabaseClasses());
            db.setName(getDescription().getName());
            getServer().configureDbConfig(db);
            
            DataSourceConfig ds = db.getDataSourceConfig();
            
            ds.setUrl(replaceDatabaseString(ds.getUrl()));
            getDataFolder().mkdirs();
            
            ClassLoader previous = Thread.currentThread().getContextClassLoader();
            
            Thread.currentThread().setContextClassLoader(previous);
            this.ebean = EbeanServerFactory.create(db);
            Thread.currentThread().setContextClassLoader(previous);
        }
    }
}
