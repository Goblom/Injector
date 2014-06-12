/*
 * Copyright 2014 Goblom.
 *
 * All Rights Reserved unless otherwise explicitly stated.
 */

package org.goblom.injector.inject;

import java.io.File;
import java.util.logging.Level;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.goblom.injector.Injector;

/**
 *
 * @author Goblom
 */
public abstract class Configable extends Injectable {
    
    private FileConfiguration injectorConfig;
    protected final File injectorConfigFile = new File(Injector.getDataFolder(), getClass().getName() + ".yml");
    private final File dataFolder = new File(Injector.getDataFolder(), getClass().getName() + "/");
    
    public File getDataFolder() {
        return dataFolder;
    }
    
    public void reloadConfig() {
        if (!injectorConfigFile.exists()) {
            createConfig();
        }
        injectorConfig = YamlConfiguration.loadConfiguration(injectorConfigFile);
    }
    
    public void saveConfig() {
        try {
            getConfig().save(injectorConfigFile);
        } catch (Exception e) {
            Injector.getLogger().log(Level.SEVERE, "Could not save config to " + injectorConfigFile + " for " + getClass().getName(), e);
        }
    }
    
    public FileConfiguration getConfig() {
        if (injectorConfig == null) {
            reloadConfig();
        }
        return injectorConfig;
    }
    
    public void createConfig() {
        if (!injectorConfigFile.exists()) {
            try {
                injectorConfigFile.createNewFile();
            } catch (Exception e) {
                Injector.getLogger().log(Level.SEVERE, "Unable to create config at " + injectorConfigFile + " for " + getClass().getName(), e);
            }
        }
    }
}
