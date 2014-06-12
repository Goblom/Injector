/*
 * Copyright 2014 Goblom.
 *
 * All Rights Reserved unless otherwise explicitly stated.
 */

package org.goblom.injector.data;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandMap;
import org.bukkit.command.CommandSender;
import org.bukkit.command.SimpleCommandMap;
import org.goblom.injector.inject.InjectableCommand;
import org.goblom.injector.reflection.FieldAccessor;
import org.goblom.injector.reflection.SafeField;

/**
 *
 * @author Goblom
 */
public class ReflectCommand extends Command {

    private InjectableCommand exe = null;
    protected static final FieldAccessor<Map<String, Command>> KNOWN_COMMANDS = new SafeField<Map<String, org.bukkit.command.Command>>(SimpleCommandMap.class, "knownCommands");
    
    public ReflectCommand(InjectableCommand exe) {
        super(ChatColor.stripColor(exe.getLabel().replaceAll(" ", "_")));
        this.exe = exe;
    }
    
    public InjectableCommand getCommand() {
        return exe;
    }
    
    @Override
    public boolean unregister(CommandMap map) {
        List<String> toRemove = new ArrayList();
        Map<String, Command> knownCommands = KNOWN_COMMANDS.get(map);
        
        if (knownCommands == null) {
            return false;
        }
        
        for (Iterator<Command> i = knownCommands.values().iterator(); i.hasNext();) {
            Command cmd = i.next();
            
            if (cmd instanceof ReflectCommand) {
                if (cmd.getLabel().equalsIgnoreCase(ChatColor.stripColor(exe.getLabel()).replaceAll(" ", "_"))) {
                    i.remove();
                    
                    toRemove.add(cmd.getLabel());
                    if (cmd.getAliases() != null) {
                        for (String alias : cmd.getAliases()) {
                            toRemove.add(alias);
                        }
                    }
                }
            }
        }
        
        for (String string : toRemove) {
            knownCommands.remove(string);
        }
        return true;
    }
    
    @Override
    public boolean execute(CommandSender cs, String label, String[] args) {
        if (exe != null) {
            return exe.execute(cs, label, args);
        }
        return false;
    }
    
    @Override
    public String getPermission() {
        if (exe.getPermission() != null) {
            return exe.getPermission();
        }
        return super.getPermission();
    }
    
    @Override
    public boolean testPermission(CommandSender target) {
        if (testPermissionSilent(target)) {
            return true;
        }
        
        if (getPermissionMessage() == null) {
            target.sendMessage(ChatColor.RED + "I'm sorry, but you do not have permission to perform this command. Please contact the server administrators if you believe that this is in error.");
        } else if (getPermissionMessage().length() != 0) {
            for (String line : getPermissionMessage().replace("<permission>", getPermission()).split("\n")) {
                target.sendMessage(line);
            }
        }
        
        return false;
    }
    
    @Override
    public boolean testPermissionSilent(CommandSender target) {
        if ((getPermission() == null) || getPermission().length() == 0) {
            return true;
        }
        
        return target.hasPermission(getPermission());
    }
    
    @Override
    public String getPermissionMessage() {
        if (exe.getPermissionMessage() != null) {
            return ChatColor.translateAlternateColorCodes('&', exe.getPermissionMessage());
        }
        
        return super.getPermissionMessage();
    }
    
    @Override
    public String getDescription() {
        if (exe.getDescription() != null) {
            return ChatColor.translateAlternateColorCodes('&', exe.getDescription());
        }
        
        return super.getDescription();
    }
    
    @Override
    public String getUsage() {
        if (exe.getUsage() != null) {
            return ChatColor.translateAlternateColorCodes('&', exe.getUsage());
        }
        
        return super.getUsage();
    }
}
