/*
 * Copyright 2014 Goblom.
 *
 * All Rights Reserved unless otherwise explicitly stated.
 */

package test;

import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.goblom.injector.factory.PluginDescriptionFactory;
import org.goblom.injector.inject.InjectablePlugin;

/**
 *
 * @author Goblom
 */
public class TestPlugin extends InjectablePlugin implements Listener {

    @Override
    public PluginDescriptionFactory getDescriptionFactory() {
        return PluginDescriptionFactory.of(this).withName("PluginTest").withVersion("Some version").withAuthors("Goblom").withDescription("Some test plugin");
    }
    
    @Override
    public void onInject() {
        Bukkit.broadcastMessage("TestPlugin#onInject()");
    }
    
    @Override
    public void onLoad() {
        super.onLoad();
        Bukkit.broadcastMessage("TestPlugin#onLoad()");
    }
    
    @Override
    public void onEnable() {
        Bukkit.broadcastMessage("TestPlugin#onEnable()");
        Bukkit.getPluginManager().registerEvents(this, this);
    }
    
    @Override
    public void onDisable() {
        Bukkit.broadcastMessage("TestPlugin#onDisable()");
    }
    
    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        Bukkit.broadcastMessage("TestPlugin#onMove(PlayerMoveEvent event)");
    }
}
