package dev.itscactus.cduello.managers;

import dev.itscactus.cduello.Main;
import dev.itscactus.cduello.models.PlayerStats;
import dev.itscactus.cduello.utils.DatabaseManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

/**
 * Oyuncu istatistiklerini yöneten sınıf
 */
public class StatsManager {
    private final Main plugin;
    private final DatabaseManager databaseManager;
    private final Map<UUID, PlayerStats> playerStats;
    private BukkitTask saveTask;
    private boolean saveScheduled;
    
    /**
     * İstatistik yöneticisini başlatır
     * 
     * @param plugin Plugin ana sınıfı
     * @param databaseManager Veritabanı yöneticisi
     */
    public StatsManager(Main plugin, DatabaseManager databaseManager) {
        this.plugin = plugin;
        this.databaseManager = databaseManager;
        this.playerStats = new HashMap<>();
        this.saveScheduled = false;
        
        // İstatistikleri yükle
        loadStats();
        
        // Düzenli kaydetme görevini başlat
        startSaveTask();
    }
    
    /**
     * Veritabanındaki tüm istatistikleri yükler
     */
    private void loadStats() {
        databaseManager.loadAllPlayerStats().thenAccept(stats -> {
            // İstatistikleri belleğe al
            playerStats.putAll(stats);
            
            if (plugin.getConfig().getBoolean("debug", false)) {
                plugin.getLogger().info(stats.size() + " oyuncunun istatistikleri bellekte yüklendi.");
            }
        });
    }
    
    /**
     * Düzenli kaydetme görevini başlatır
     */
    private void startSaveTask() {
        // Eski görevi iptal et
        if (saveTask != null) {
            saveTask.cancel();
        }
        
        // Yeni görevi başlat
        int saveInterval = plugin.getConfig().getInt("stats.save-interval", 300) * 20; // Tick cinsinden
        
        saveTask = Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, this::saveAllStats, saveInterval, saveInterval);
    }
    
    /**
     * Tüm oyuncu istatistiklerini veritabanına kaydeder
     */
    public void saveAllStats() {
        if (playerStats.isEmpty()) {
            return;
        }
        
        if (plugin.getConfig().getBoolean("debug", false)) {
            plugin.getLogger().info("Tüm oyuncu istatistikleri kaydediliyor...");
        }
        
        for (Map.Entry<UUID, PlayerStats> entry : playerStats.entrySet()) {
            UUID uuid = entry.getKey();
            PlayerStats stats = entry.getValue();
            
            // Oyuncu adını al
            String playerName = Bukkit.getOfflinePlayer(uuid).getName();
            if (playerName == null) {
                playerName = uuid.toString();
            }
            
            // Veritabanına kaydet
            databaseManager.savePlayerStats(stats, playerName).exceptionally(ex -> {
                plugin.getLogger().log(Level.SEVERE, "Oyuncu istatistikleri kaydedilirken hata: " + uuid, ex);
                return false;
            });
        }
    }
    
    /**
     * Oyuncu istatistiklerini veritabanına kaydeder
     * 
     * @param player Oyuncu
     */
    public void savePlayerStats(Player player) {
        UUID uuid = player.getUniqueId();
        if (!playerStats.containsKey(uuid)) {
            return;
        }
        
        PlayerStats stats = playerStats.get(uuid);
        databaseManager.savePlayerStats(stats, player.getName()).exceptionally(ex -> {
            plugin.getLogger().log(Level.SEVERE, "Oyuncu istatistikleri kaydedilirken hata: " + player.getName(), ex);
            return false;
        });
    }
    
    /**
     * Bir oyuncunun istatistiklerini alır, yoksa yükler
     * 
     * @param player Oyuncu
     * @return İstatistikler
     */
    public CompletableFuture<PlayerStats> getPlayerStats(Player player) {
        UUID uuid = player.getUniqueId();
        
        // Eğer belleğe yüklenmişse hemen döndür
        if (playerStats.containsKey(uuid)) {
            return CompletableFuture.completedFuture(playerStats.get(uuid));
        }
        
        // Veritabanından yükle
        return databaseManager.loadPlayerStats(uuid).thenApply(stats -> {
            // Belleğe ekle
            playerStats.put(uuid, stats);
            return stats;
        });
    }
    
    /**
     * Bir oyuncunun kazanmasını kaydeder
     * 
     * @param player Kazanan oyuncu
     * @param bet Bahis miktarı
     */
    public void recordWin(Player player, double bet) {
        getPlayerStats(player).thenAccept(stats -> {
            stats.incrementWins();
            stats.addMoneyWon(bet);
            
            // Otomatik kaydetme
            scheduleSave();
        });
    }
    
    /**
     * Bir oyuncunun kaybetmesini kaydeder
     * 
     * @param player Kaybeden oyuncu
     * @param bet Bahis miktarı
     */
    public void recordLoss(Player player, double bet) {
        getPlayerStats(player).thenAccept(stats -> {
            stats.incrementLosses();
            stats.addMoneyLost(bet);
            
            // Otomatik kaydetme
            scheduleSave();
        });
    }
    
    /**
     * Otomatik kaydetmeyi planlar
     */
    private void scheduleSave() {
        // Eğer zaten bir kayıt planlanmışsa tekrar planlama
        if (saveScheduled) {
            return;
        }
        
        saveScheduled = true;
        
        // 1 saniye sonra kaydet
        Bukkit.getScheduler().runTaskLaterAsynchronously(plugin, () -> {
            saveAllStats();
            saveScheduled = false;
        }, 20L);
    }
    
    /**
     * Plugin devre dışı bırakıldığında istatistikleri kaydet
     */
    public void shutdown() {
        // Kayıt görevini iptal et
        if (saveTask != null) {
            saveTask.cancel();
        }
        
        // Senkron olarak tüm verileri kaydet
        saveAllStats();
    }
} 