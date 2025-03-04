package dev.itscactus.cduello;

import dev.itscactus.cduello.commands.DuelloCommand;
import dev.itscactus.cduello.listeners.DuelListener;
import dev.itscactus.cduello.listeners.PlayerListener;
import dev.itscactus.cduello.managers.DuelManager;
import dev.itscactus.cduello.managers.EconomyManager;
import dev.itscactus.cduello.utils.MessageManager;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.TabCompleter;
import org.bukkit.event.Listener;

public class Main extends JavaPlugin {

    private static Main instance;
    private DuelManager duelManager;
    private EconomyManager economyManager;
    private MessageManager messageManager;

    @Override
    public void onEnable() {
        instance = this;
        
        // Save default config
        saveDefaultConfig();
        
        // Initialize utils
        messageManager = new MessageManager(this);
        
        // Initialize managers
        economyManager = new EconomyManager(this);
        duelManager = new DuelManager(this, economyManager);
        
        // Register commands
        registerCommands();
        
        // Register events
        registerEvents();
        
        getLogger().info("cDuello plugin has been enabled!");
    }

    @Override
    public void onDisable() {
        // Adventure API kaynaklarını serbest bırak
        if (messageManager != null) {
            messageManager.close();
        }
        
        getLogger().info("cDuello plugin has been disabled!");
    }
    
    private void registerCommands() {
        DuelloCommand duelloCommand = new DuelloCommand(this, duelManager);
        registerCommand("duello", duelloCommand, duelloCommand);
    }
    
    private void registerEvents() {
        registerListener(new DuelListener(this, duelManager));
        registerListener(new PlayerListener(this, duelManager));
    }
    
    /**
     * Registers a command with optional tab completer
     * 
     * @param name The command name
     * @param executor The command executor
     * @param completer The tab completer (can be null)
     */
    private void registerCommand(String name, CommandExecutor executor, TabCompleter completer) {
        getCommand(name).setExecutor(executor);
        if (completer != null) {
            getCommand(name).setTabCompleter(completer);
        }
    }
    
    /**
     * Registers a listener
     * 
     * @param listener The listener to register
     */
    private void registerListener(Listener listener) {
        Bukkit.getPluginManager().registerEvents(listener, this);
    }
    
    /**
     * Gets the instance of the main plugin class
     * 
     * @return The main plugin instance
     */
    public static Main getInstance() {
        return instance;
    }
    
    /**
     * Gets the duel manager
     * 
     * @return The duel manager
     */
    public DuelManager getDuelManager() {
        return duelManager;
    }
    
    /**
     * Gets the economy manager
     * 
     * @return The economy manager
     */
    public EconomyManager getEconomyManager() {
        return economyManager;
    }
    
    /**
     * Gets the message manager
     * 
     * @return The message manager
     */
    public MessageManager getMessageManager() {
        return messageManager;
    }
} 