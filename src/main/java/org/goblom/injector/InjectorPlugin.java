/*
 * Copyright 2014 Goblom.
 *
 * All Rights Reserved unless otherwise explicitly stated.
 */

package org.goblom.injector;

import com.google.common.collect.Lists;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandMap;
import org.bukkit.command.CommandSender;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginDescriptionFile;
import org.bukkit.plugin.java.JavaPlugin;
import org.goblom.injector.data.ReflectCommand;
import org.goblom.injector.events.CommandInjectEvent;
import org.goblom.injector.events.InjectEvent;
import org.goblom.injector.events.PluginInjectEvent;
import org.goblom.injector.events.UnloadEvent;
import org.goblom.injector.factory.CommandRegistrationFactory;
import org.goblom.injector.inject.Informable;
import org.goblom.injector.inject.Injectable;
import org.goblom.injector.inject.InjectableCommand;
import org.goblom.injector.inject.InjectablePlugin;
import org.goblom.injector.inject.Unloadable;
import org.goblom.injector.plugin.InjectorPluginLoader;

/**
 *
 * @author Goblom
 */
public class InjectorPlugin extends JavaPlugin implements InjectorAPI {

    private List<Injectable> injected = Lists.newArrayList();
    private CommandMap commandMap = null;

    @Override
    public void onLoad() {
        Injector.setInjector(this);
    }

    @Override
    public void onEnable() {
        saveResource("InjectorHelp.txt", true);
        saveResource("TestPlugin.class", true);

        try {
            Metrics met = new Metrics(this);
            met.start();
        } catch (IOException e) {
            getLogger().info("Metrics will not be tracked this time :(");
        }

        Bukkit.getPluginManager().registerInterface(InjectorPluginLoader.class);

        try {
            final Field commandMap = Bukkit.getServer().getClass().getDeclaredField("commandMap");
            commandMap.setAccessible(true);

            this.commandMap = (CommandMap) commandMap.get(Bukkit.getServer());
        } catch (Exception e) {
            e.printStackTrace();
            getLogger().severe("Unable to load CommandMap, InjectableCommands will not be injected.");
        }

        CommandRegistrationFactory commandFactory = CommandRegistrationFactory.buildCommand("injector");
                                   commandFactory.withAliases("inject");
                                   commandFactory.withCommandExecutor(this);
                                   commandFactory.withPlugin(this);
                                   commandFactory.withDescription("Injector Admin Command");

                                   commandFactory.register();
        load();
    }

    @Override
    public void onDisable() {
        Iterator<Injectable> it = injected.iterator();

        while (it.hasNext()) {
            Injectable i = it.next();

            if (i instanceof InjectablePlugin) {
                Bukkit.getPluginManager().disablePlugin((InjectablePlugin) i);
            }

            if (i instanceof Listener) {
                HandlerList.unregisterAll((Listener) i);
            }

            try {
                i.setInjected(false);
            } catch (Throwable t) {
                t.printStackTrace();
            }

            it.remove();
        }
    }
    public CommandMap getCommandMap() {
        return commandMap;
    }

    @Override
    public boolean canInjectCommands() {
        return getCommandMap() != null;
    }

    @Override
    public boolean inject(Class<? extends Injectable> inject) {
        if (!isClassInjected(inject)) {
            try {
                Injectable i = inject.getConstructor().newInstance();

                if (i instanceof InjectablePlugin) {
                    PluginInjectEvent pie = new PluginInjectEvent((InjectablePlugin) i);

                    Bukkit.getPluginManager().callEvent(pie);

                    if (!pie.isCancelled()) {
                        InjectablePlugin ip = pie.getInjectedPlugin();
                        try {
                            ip.setInjected(true);
                        } catch (Throwable t) {
                            t.printStackTrace();
                        }

                        ip.getPluginLoader().enablePlugin(ip);
//                        Bukkit.getPluginManager().enablePlugin(ip);

                        injected.add(ip);
                        return true;
                    } else return false;
                } else if (i instanceof InjectableCommand) {
                    if (!canInjectCommands()) {
                        return false;
                    }

                    CommandInjectEvent cie = new CommandInjectEvent((InjectableCommand) i);

                    Bukkit.getPluginManager().callEvent(cie);

                    if (!cie.isCancelled()) {
                        InjectableCommand ic = cie.getInjectedCommand();

                        ReflectCommand cmd = new ReflectCommand(ic);

                        if (ic.getAliases() != null) {
                            cmd.setAliases(ic.getAliases());
                        }

                        if (ic.getDescription() != null) {
                            cmd.setDescription(ic.getDescription());
                        }

                        if (ic.getPermission() != null) {
                            cmd.setDescription(ic.getDescription());
                        }

                        if (ic.getPermissionMessage() != null) {
                            cmd.setPermissionMessage(ic.getPermissionMessage());
                        }

                        if (ic.getUsage() != null) {
                            cmd.setUsage(ic.getUsage());
                        }

                        getCommandMap().register("injector", cmd);

                        try {
                            ic.setInjected(true);
                        } catch (Throwable t) {
                            t.printStackTrace();
                        }
                        injected.add(ic);
                        return true;
                    } else return false;
                } else {
                    InjectEvent ie = new InjectEvent(i);

                    Bukkit.getPluginManager().callEvent(ie);

                    if (!ie.isCancelled()) {
                        Injectable ij = ie.getInjected();

                        try {
                            ij.setInjected(true);
                        } catch (Throwable t) {
                            t.printStackTrace();
                        }
                        injected.add(ij);
                        return true;
                    } else return false;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return false;
    }

    @Override
    public boolean unload(Class<? extends Unloadable> clazz) {
        for (Injectable i : getInjected()) {
            if (i.getClass().getName().equalsIgnoreCase(clazz.getName())) {
                if (i instanceof Unloadable) {
                    UnloadEvent ue = new UnloadEvent((Unloadable) i);

                    Bukkit.getPluginManager().callEvent(ue);

                    if (!ue.isCancelled()) {
                        i.setInjected(false);

                        if (i instanceof Listener) {
                            HandlerList.unregisterAll((Listener) i);
                        }

                        if (i instanceof InjectableCommand) {
                            getCommandMap().getCommand(ChatColor.stripColor(((InjectableCommand) i).getLabel().replace(" ", "_"))).unregister(getCommandMap());
                        }

                        if (i instanceof InjectablePlugin) {
                            try {
                                Bukkit.getPluginManager().disablePlugin((InjectablePlugin) i);
                            } catch (Exception e) {
                                Bukkit.getPluginManager().disablePlugin((InjectablePlugin) i);
                            }
                        }

                        injected.remove(i);
                    }
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public List<Injectable> getInjected() {
        return Collections.unmodifiableList(injected);
    }

    @Override
    public Injectable getInjectable(String name) {
        for (Injectable i : getInjected()) {
            if (i.getName().equals(name)) {
                return i;
            }
        }

        throw new RuntimeException("Unable to get Injectable with name " + name);
    }

    public boolean isClassInjected(Class<? extends Injectable> clazz) {
        for (Injectable i : getInjected()) {
            if (i.getClass().getName().equalsIgnoreCase(clazz.getName())) {
                return true;
            }
        }

        return false;
    }

    public final void load() {
        if (!getDataFolder().exists()) {
            getDataFolder().mkdirs();
        }

        ClassLoader loader;

        try {
            loader = new URLClassLoader(new URL[] { getDataFolder().toURI().toURL() }, Injectable.class.getClassLoader());
        } catch (Exception e) {
            e.printStackTrace();
            getLogger().severe("Unable to load ClassLoader, not loading files from data folder");
            return;
        }

        for (File file : getDataFolder().listFiles()) {
            if (!file.getName().endsWith(".class")) {
                continue; //only allow class files... for now
            }

            String name = file.getName().substring(0, file.getName().indexOf("."));

            try {
                Class clazz = loader.loadClass(name);
                Object o = clazz.newInstance();

                if (!(o instanceof Injectable)) {
                    getLogger().warning(clazz.getSimpleName() + " is not an Injectable class!");
                    continue;
                }

                if (o instanceof Listener) {
                    getLogger().info(clazz.getSimpleName() + " is also a listener. this might cause problems in the future.");
                }

                if (o instanceof InjectablePlugin) {
                    Plugin plugin = Bukkit.getPluginManager().loadPlugin(file);

                    if (plugin != null) {
                        Bukkit.getPluginManager().enablePlugin(plugin);

                        if (plugin instanceof Injectable) {
                            Injectable inj = (Injectable) plugin;

                            try {
                                inj.setInjected(true);
                            } catch (Throwable t) {
                                t.printStackTrace();
                            }

                            injected.add(inj);
                        } else {
                            getLogger().warning(plugin.getName() + " was injected but not added to the list, no data will show about it in /inject [list/info]");
                        }
                    } else {
                        getLogger().warning("Unable to load InjectablePlugin " + ((Injectable) o).getName() + " is null");
                    }
                    continue;
                }

                inject(clazz);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (sender.hasPermission("injector.admin") || sender.isOp()) {
            if (args.length >= 1) {
                switch (args[0].toLowerCase()) {
                    case "plugin":
                        if (args.length >= 3) {
                            InjectablePlugin plugin;

                            Plugin p = Bukkit.getPluginManager().getPlugin(args[1]);

                            if (p instanceof InjectablePlugin) {
                                plugin = (InjectablePlugin) p;
                            } else {
                                sendMessage(sender, ChatColor.RED + p.getName() + " is not an InjectablePlugin.");
                                break;
                            }

                            if (plugin == null) {
                                sendMessage(sender, ChatColor.RED + args[1] + " does not exist or is not injected.");
                                break;
                            }
                            switch (args[2].toLowerCase()) {
                                case "info":
                                    sendTitle(sender, "InjectedPlugin Info");
                                    PluginDescriptionFile description = plugin.getDescription();

                                    if (description.getVersion() != null) {
                                        sendMessage(sender, "Version: " + description.getVersion());
                                    }

                                    if (description.getDescription() != null) {
                                        sendMessage(sender, "Description: " + description.getDescription());
                                    }

                                    if (description.getWebsite() != null) {
                                        sendMessage(sender, "Website: " + description.getWebsite());
                                    }

                                    if (description.getAuthors() != null && !description.getAuthors().isEmpty()) {
                                        sendMessage(sender, "Authors:");

                                        for (String author : description.getAuthors()) {
                                            sendMessage(sender, ChatColor.BLUE + "-" + ChatColor.GOLD + "    " + author);
                                        }
                                    }
                                    break;
                                case "inject":
                                case "unload":
                                case "disable":
                                    if (plugin.isEnabled() && plugin.isInjected()) {
                                        Bukkit.getPluginManager().disablePlugin(plugin);
                                        sendMessage(sender, plugin.isEnabled() ? ChatColor.RED + "There seems to have been a problem when trying to disable the plugin." : ChatColor.GREEN + "Plugin successfully disabled!");
                                    } else {
                                        sendMessage(sender, ChatColor.RED + p.getName() + " is already disabled.");
                                    }
                                    break;
                                default:
                                    sendMessage(sender, ChatColor.RED + "This command is currently under development");
                                    break;
                            }
                        }
                        break;
                    case "list":
                        sendTitle(sender, "Injected Files");
                        for (Injectable i : getInjected()) {
                            sendMessage(sender, ChatColor.BLUE + "- " + (i.isInjected() ? ChatColor.GREEN : ChatColor.RED) + i.getClass().getName());
                        }
                        break;
                    case "info":
                        List<InjectablePlugin> injectedPlugins = Lists.newArrayList();
                        List<InjectableCommand> injectedCommands = Lists.newArrayList();
                        List<Informable> injectedInformation = Lists.newArrayList();

                        sendTitle(sender, "Injected Descriptions");

                        for (Injectable i : getInjected()) {
                            if (i instanceof InjectableCommand) {
                                injectedCommands.add((InjectableCommand) i);
                            } else if (i instanceof InjectablePlugin) {
                                injectedPlugins.add((InjectablePlugin) i);
                            } else if (i instanceof Informable) {
                                injectedInformation.add((Informable) i);
                            } else {
                                sendMessage(sender, ChatColor.GOLD + "- " + (i.isInjected() ? ChatColor.GREEN : ChatColor.RED) + i.getClass().getName() + ChatColor.GOLD + " -- " + ChatColor.DARK_RED + "No Information");
                            }
                        }

                        sendMessage(sender, ChatColor.BLUE + "Plugins: ");
                        for (InjectablePlugin iPlugin : injectedPlugins) {
                            try {
                                sendMessage(sender, ChatColor.GOLD + "- " + (iPlugin.isInjected() && iPlugin.isEnabled() ? ChatColor.GREEN : ChatColor.RED) + iPlugin.getName() + ChatColor.GOLD + " -- " + iPlugin.getDescription().getDescription());
                            } catch (Exception e) {}
                        }

                        sendMessage(sender, ChatColor.BLUE + "Commands: ");
                        for (InjectableCommand command : injectedCommands) {
                            sendMessage(sender, ChatColor.BLUE + "- " + (command.isInjected() ? ChatColor.GREEN : ChatColor.RED) + command.getClass().getName() + " with command '" + command.getLabel() + "'");
                        }

                        sendMessage(sender, ChatColor.BLUE + "Other: ");
                        for (Informable i : injectedInformation) {
                            try {
                                Injectable in = (Injectable) i;
                                sendMessage(sender, ChatColor.BLUE + "- " + (in.isInjected() ? ChatColor.GREEN : ChatColor.RED) + i.getClass().getName() + ChatColor.GOLD + " -- " + ChatColor.BLUE + i.getInformation().getAuthor());
                            } catch (Exception e) {}
                        }
                        break;
                    default:
                        sendCommandUsage(sender);
                        break;
                }
            } else {
                sendCommandUsage(sender);
            }
        } else {
            sendMessage(sender, ChatColor.RED + "You do not have permission to use that command.");
        }
        return false;
    }

    private void sendMessage(CommandSender sender, String message) {
        sender.sendMessage(ChatColor.DARK_GRAY + "[" + ChatColor.DARK_AQUA + "Injector" + ChatColor.DARK_GRAY + "] " + ChatColor.RESET + message);
    }

    @Override
    public Plugin getBukkit() {
        return this;
    }

    private void sendCommandUsage(CommandSender sender) {
        sendMessage(sender, ChatColor.RED + "Usage:");
        sendMessage(sender, ChatColor.RED + "       /inject info");
        sendMessage(sender, ChatColor.RED + "       /inject list");
        sendMessage(sender, ChatColor.RED + "       /inject plugin <plugin> [info/inject/unload/disable]");
    }

    private void sendTitle(CommandSender sender, String title) {
        sendMessage(sender, ChatColor.GREEN + "---------------------------");
        sendMessage(sender, ChatColor.BLUE + title);
        sendMessage(sender, ChatColor.GREEN + "---------------------------");
    }
}
