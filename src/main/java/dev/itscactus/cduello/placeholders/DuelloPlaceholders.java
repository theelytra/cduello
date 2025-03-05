package dev.itscactus.cduello.placeholders;

import dev.itscactus.cduello.Main;
import dev.itscactus.cduello.managers.StatsManager;
import dev.itscactus.cduello.models.PlayerStats;
import dev.itscactus.cduello.utils.DatabaseManager;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.text.DecimalFormat;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;

/**
 * PlaceholderAPI entegrasyonu için sınıf
 */
public class DuelloPlaceholders extends PlaceholderExpansion {
    private final Main plugin;
    private final StatsManager statsManager;
    private final DatabaseManager databaseManager;
    private final DecimalFormat decimalFormat = new DecimalFormat("#,##0.00");
    private final DecimalFormat percentFormat = new DecimalFormat("#0.0%");
    
    // Önbellek
    private final Map<Integer, String> leaderboardCache = new ConcurrentHashMap<>();
    private long lastCacheUpdate = 0;
    private static final long CACHE_DURATION = 60 * 1000; // 1 dakika (milisaniye)
    
    /**
     * PlaceholderAPI için yapıcı metot
     * 
     * @param plugin Plugin ana sınıfı
     * @param statsManager İstatistik yöneticisi
     * @param databaseManager Veritabanı yöneticisi
     */
    public DuelloPlaceholders(Main plugin, StatsManager statsManager, DatabaseManager databaseManager) {
        this.plugin = plugin;
        this.statsManager = statsManager;
        this.databaseManager = databaseManager;
    }

    @Override
    public String getIdentifier() {
        return "duello";
    }

    @Override
    public String getAuthor() {
        return plugin.getDescription().getAuthors().get(0);
    }

    @Override
    public String getVersion() {
        return plugin.getDescription().getVersion();
    }
    
    @Override
    public boolean persist() {
        return true;
    }
    
    @Override
    public String onRequest(OfflinePlayer offlinePlayer, String params) {
        // Oyuncu ile ilgili placeholderlar
        if (offlinePlayer != null && offlinePlayer.isOnline()) {
            Player player = offlinePlayer.getPlayer();
            
            if (params.equals("wins")) {
                try {
                    PlayerStats stats = statsManager.getPlayerStats(player).get();
                    return String.valueOf(stats.getWins());
                } catch (InterruptedException | ExecutionException e) {
                    plugin.getLogger().log(Level.SEVERE, "Placeholder için istatistik alınırken hata oluştu", e);
                    return "0";
                }
            }
            
            if (params.equals("losses")) {
                try {
                    PlayerStats stats = statsManager.getPlayerStats(player).get();
                    return String.valueOf(stats.getLosses());
                } catch (InterruptedException | ExecutionException e) {
                    plugin.getLogger().log(Level.SEVERE, "Placeholder için istatistik alınırken hata oluştu", e);
                    return "0";
                }
            }
            
            if (params.equals("total_duels")) {
                try {
                    PlayerStats stats = statsManager.getPlayerStats(player).get();
                    return String.valueOf(stats.getTotalDuels());
                } catch (InterruptedException | ExecutionException e) {
                    plugin.getLogger().log(Level.SEVERE, "Placeholder için istatistik alınırken hata oluştu", e);
                    return "0";
                }
            }
            
            if (params.equals("win_ratio")) {
                try {
                    PlayerStats stats = statsManager.getPlayerStats(player).get();
                    return percentFormat.format(stats.getWinRatio());
                } catch (InterruptedException | ExecutionException e) {
                    plugin.getLogger().log(Level.SEVERE, "Placeholder için istatistik alınırken hata oluştu", e);
                    return "0%";
                }
            }
            
            if (params.equals("money_won")) {
                try {
                    PlayerStats stats = statsManager.getPlayerStats(player).get();
                    return decimalFormat.format(stats.getMoneyWon());
                } catch (InterruptedException | ExecutionException e) {
                    plugin.getLogger().log(Level.SEVERE, "Placeholder için istatistik alınırken hata oluştu", e);
                    return "0.00";
                }
            }
            
            if (params.equals("money_lost")) {
                try {
                    PlayerStats stats = statsManager.getPlayerStats(player).get();
                    return decimalFormat.format(stats.getMoneyLost());
                } catch (InterruptedException | ExecutionException e) {
                    plugin.getLogger().log(Level.SEVERE, "Placeholder için istatistik alınırken hata oluştu", e);
                    return "0.00";
                }
            }
        }
        
        // Sıralama placeholderları
        if (params.startsWith("siralama_")) {
            try {
                // Sıralama numarasını al
                int position = Integer.parseInt(params.substring("siralama_".length()));
                
                // Önbellekten al
                return getLeaderboardPlaceholder(position);
            } catch (NumberFormatException e) {
                return "Geçersiz Sıralama";
            }
        }
        
        return null;
    }
    
    /**
     * Sıralama placeholderını cache mekanizması ile alır
     * 
     * @param position Sıralama pozisyonu (1'den başlar)
     * @return Sıralamadaki oyuncu adı, yoksa "Yok"
     */
    private String getLeaderboardPlaceholder(int position) {
        // Cache süresi dolmuşsa güncelle
        if (System.currentTimeMillis() - lastCacheUpdate > CACHE_DURATION) {
            updateLeaderboardCache();
        }
        
        // Cache'den al
        return leaderboardCache.getOrDefault(position, "Yok");
    }
    
    /**
     * Sıralama önbelleğini günceller
     */
    private void updateLeaderboardCache() {
        for (int i = 1; i <= 10; i++) {
            final int position = i;
            
            // Asenkron olarak al ve cache'e ekle
            databaseManager.getPlayerAtRank(position, "wins").thenAccept(entry -> {
                if (entry != null) {
                    leaderboardCache.put(position, entry.getValue());
                } else {
                    leaderboardCache.put(position, "Yok");
                }
            });
        }
        
        // Cache güncelleme zamanını kaydet
        lastCacheUpdate = System.currentTimeMillis();
    }
} 