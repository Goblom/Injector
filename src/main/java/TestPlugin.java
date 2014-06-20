
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.goblom.injector.factory.PluginDescriptionFactory;
import org.goblom.injector.inject.InjectablePlugin;

/*
 * Copyright 2014 Goblom.
 *
 * All Rights Reserved unless otherwise explicitly stated.
 */

/**
 *
 * @author Goblom
 */
public class TestPlugin extends InjectablePlugin implements Listener {

    @Override
    public PluginDescriptionFactory getDescriptionFactory() {
        return PluginDescriptionFactory.of(this).withName("TestPlugin").withAuthors("Goblom").withVersion("1.0");
    }
    
    @Override
    public void onEnable() {
        Bukkit.getPluginManager().registerEvents(this, this);
        
        getLogger().warning("TestPlugin#onEnable");
    }
    
    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        Bukkit.broadcastMessage(event.getPlayer().getName() + " has moved!");
    }
}
