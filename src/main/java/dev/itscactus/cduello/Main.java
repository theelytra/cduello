package dev.itscactus.cduello;

import dev.itscactus.cduello.commands.DuelAdminCommand;
import dev.itscactus.cduello.commands.DuelloCommand;
import dev.itscactus.cduello.listeners.DuelListener;
import dev.itscactus.cduello.listeners.LeaderboardListener;
import dev.itscactus.cduello.managers.ArenaManager;
import dev.itscactus.cduello.managers.DuelManager;
import dev.itscactus.cduello.managers.EconomyManager;
import dev.itscactus.cduello.managers.StatsManager;
import dev.itscactus.cduello.placeholders.DuelloPlaceholders;
import dev.itscactus.cduello.utils.DatabaseManager;
import dev.itscactus.cduello.utils.MessageManager;
import org.bukkit.Bukkit;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

public class Main extends JavaPlugin {

    private DatabaseManager databaseManager;
    private MessageManager messageManager;
    private ArenaManager arenaManager;
    private DuelManager duelManager;
    private StatsManager statsManager;
    private EconomyManager economyManager;

    @Override
    public void onEnable() {
        // Config dosyasını yükle
        saveDefaultConfig();
        
        // Veritabanı yapılandırmasını kontrol et
        if (!getConfig().contains("database.migration-completed")) {
            getConfig().set("database.migration-completed", false);
            saveConfig();
        }
        
        // Veritabanı yöneticisini başlat
        databaseManager = new DatabaseManager(this);
        
        // Mesaj yöneticisini başlat
        messageManager = new MessageManager(this);
        
        // Ekonomi yöneticisini başlat
        economyManager = new EconomyManager(this);
        
        // Arena yöneticisini başlat
        arenaManager = new ArenaManager(this);
        
        // İstatistik yöneticisini başlat
        statsManager = new StatsManager(this, databaseManager);
        
        // Düello yöneticisini başlat
        duelManager = new DuelManager(this, economyManager);
        
        // Arena yöneticisini düello yöneticisine ayarla
        duelManager.setArenaManager(arenaManager);
        
        // Komutları kaydet
        getCommand("duello").setExecutor(new DuelloCommand(this, duelManager));
        getCommand("dueladmin").setExecutor(new DuelAdminCommand(this, arenaManager));
        
        // Dinleyicileri kaydet
        PluginManager pluginManager = getServer().getPluginManager();
        DuelListener duelListener = new DuelListener(this, duelManager);
        pluginManager.registerEvents(duelListener, this);
        pluginManager.registerEvents(new LeaderboardListener(this, databaseManager), this);
        
        // DuelListener'ı DuelManager'a ayarla
        duelManager.setDuelListener(duelListener);
        
        // PlaceholderAPI entegrasyonu
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new DuelloPlaceholders(this, statsManager, databaseManager).register();
            getLogger().info("PlaceholderAPI entegrasyonu aktif edildi!");
        }
        
        getLogger().info("cDuello plugin aktivated");
    }

    @Override
    public void onDisable() {
        // Aktif düelloları iptal et
        if (duelManager != null) {
            duelManager.endAllDuels("Plugin devre dışı bırakıldı");
        }
        
        // İstatistikleri kaydet
        if (statsManager != null) {
            statsManager.shutdown();
        }
        
        // Veritabanı bağlantısını kapat
        if (databaseManager != null) {
            databaseManager.closeConnection();
        }
        
        getLogger().info("cDuello plugin deaktif edildi!");
    }

    public MessageManager getMessageManager() {
        return messageManager;
    }

    public ArenaManager getArenaManager() {
        return arenaManager;
    }

    public DuelManager getDuelManager() {
        return duelManager;
    }
    
    public StatsManager getStatsManager() {
        return statsManager;
    }
    
    public DatabaseManager getDatabaseManager() {
        return databaseManager;
    }
    
    public EconomyManager getEconomyManager() {
        return economyManager;
    }
} 