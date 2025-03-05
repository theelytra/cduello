package dev.itscactus.cduello.utils;

import dev.itscactus.cduello.Main;
import dev.itscactus.cduello.models.Arena;
import dev.itscactus.cduello.models.PlayerStats;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

import java.io.File;
import java.sql.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

/**
 * SQLite veritabanı bağlantısını ve işlemlerini yöneten sınıf
 */
public class DatabaseManager {
    private final Main plugin;
    private Connection connection;
    private String dbFile;

    /**
     * Veritabanı yöneticisini başlatır
     * 
     * @param plugin Plugin ana sınıfı
     */
    public DatabaseManager(Main plugin) {
        this.plugin = plugin;
        this.dbFile = new File(plugin.getDataFolder(), "database.db").getAbsolutePath();
        
        // Veritabanını başlat
        initDatabase();
    }

    /**
     * Veritabanı bağlantısını ve tablolarını oluşturur
     */
    private void initDatabase() {
        try {
            if (connection != null && !connection.isClosed()) {
                return;
            }

            // SQLite JDBC sürücüsünü yükle
            Class.forName("org.sqlite.JDBC");
            
            // Bağlantı kur
            connection = DriverManager.getConnection("jdbc:sqlite:" + dbFile);
            
            // Debug mesajı
            if (plugin.getConfig().getBoolean("debug", false)) {
                plugin.getLogger().info("SQLite veritabanı bağlantısı başarıyla kuruldu.");
            }
            
            // Tabloları oluştur
            try (Statement statement = connection.createStatement()) {
                // Arena tablosu
                statement.execute(
                    "CREATE TABLE IF NOT EXISTS arenas (" +
                    "id TEXT PRIMARY KEY, " +
                    "name TEXT NOT NULL, " +
                    "world TEXT NOT NULL, " +
                    "pos1_x DOUBLE NOT NULL, " +
                    "pos1_y DOUBLE NOT NULL, " +
                    "pos1_z DOUBLE NOT NULL, " +
                    "pos2_x DOUBLE NOT NULL, " +
                    "pos2_y DOUBLE NOT NULL, " +
                    "pos2_z DOUBLE NOT NULL, " +
                    "enabled INTEGER NOT NULL DEFAULT 1" +
                    ");"
                );
                
                // İstatistikler tablosu
                statement.execute(
                    "CREATE TABLE IF NOT EXISTS player_stats (" +
                    "uuid TEXT PRIMARY KEY, " +
                    "player_name TEXT NOT NULL, " +
                    "wins INTEGER NOT NULL DEFAULT 0, " +
                    "losses INTEGER NOT NULL DEFAULT 0, " +
                    "money_won DOUBLE NOT NULL DEFAULT 0, " +
                    "money_lost DOUBLE NOT NULL DEFAULT 0, " +
                    "last_updated TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                    ");"
                );
            }
        } catch (ClassNotFoundException | SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Veritabanı başlatılırken hata oluştu!", e);
        }
    }

    /**
     * Veritabanı bağlantısını kapatır
     */
    public void closeConnection() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Veritabanı bağlantısı kapatılırken hata oluştu!", e);
        }
    }

    /**
     * Veritabanından tüm arenaları yükler
     * 
     * @return Arena haritası
     */
    public CompletableFuture<Map<String, Arena>> loadArenas() {
        CompletableFuture<Map<String, Arena>> future = new CompletableFuture<>();
        Map<String, Arena> arenas = new HashMap<>();
        
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                initDatabase(); // Bağlantıyı kontrol et
                
                try (Statement statement = connection.createStatement();
                     ResultSet resultSet = statement.executeQuery("SELECT * FROM arenas")) {
                    
                    while (resultSet.next()) {
                        String id = resultSet.getString("id");
                        String name = resultSet.getString("name");
                        String worldName = resultSet.getString("world");
                        
                        double pos1X = resultSet.getDouble("pos1_x");
                        double pos1Y = resultSet.getDouble("pos1_y");
                        double pos1Z = resultSet.getDouble("pos1_z");
                        
                        double pos2X = resultSet.getDouble("pos2_x");
                        double pos2Y = resultSet.getDouble("pos2_y");
                        double pos2Z = resultSet.getDouble("pos2_z");
                        
                        boolean enabled = resultSet.getInt("enabled") == 1;
                        
                        World world = Bukkit.getWorld(worldName);
                        
                        if (world == null) {
                            plugin.getLogger().warning("Arena için dünya bulunamadı: " + worldName);
                            continue;
                        }
                        
                        Location pos1 = new Location(world, pos1X, pos1Y, pos1Z);
                        Location pos2 = new Location(world, pos2X, pos2Y, pos2Z);
                        
                        Arena arena = new Arena(id, name, world, pos1, pos2);
                        arena.setEnabled(enabled);
                        
                        arenas.put(id, arena);
                    }
                    
                    future.complete(arenas);
                }
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Arenalar yüklenirken hata oluştu!", e);
                future.completeExceptionally(e);
            }
        });
        
        return future;
    }

    /**
     * Arena ekler veya günceller
     * 
     * @param arena Arena
     * @return İşlem başarılı olursa true döner
     */
    public CompletableFuture<Boolean> saveArena(Arena arena) {
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                initDatabase(); // Bağlantıyı kontrol et
                
                String query = "INSERT OR REPLACE INTO arenas (id, name, world, pos1_x, pos1_y, pos1_z, pos2_x, pos2_y, pos2_z, enabled) " +
                               "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
                
                try (PreparedStatement ps = connection.prepareStatement(query)) {
                    ps.setString(1, arena.getId());
                    ps.setString(2, arena.getName());
                    ps.setString(3, arena.getWorldName());
                    
                    Location pos1 = arena.getPos1();
                    ps.setDouble(4, pos1.getX());
                    ps.setDouble(5, pos1.getY());
                    ps.setDouble(6, pos1.getZ());
                    
                    Location pos2 = arena.getPos2();
                    ps.setDouble(7, pos2.getX());
                    ps.setDouble(8, pos2.getY());
                    ps.setDouble(9, pos2.getZ());
                    
                    ps.setInt(10, arena.isEnabled() ? 1 : 0);
                    
                    ps.executeUpdate();
                    future.complete(true);
                }
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Arena kaydedilirken hata oluştu: " + arena.getId(), e);
                future.complete(false);
            }
        });
        
        return future;
    }

    /**
     * Tüm arenaları kaydeder
     * 
     * @param arenas Arena haritası
     * @return İşlem başarılı olursa true döner
     */
    public CompletableFuture<Boolean> saveAllArenas(Map<String, Arena> arenas) {
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                initDatabase(); // Bağlantıyı kontrol et
                
                connection.setAutoCommit(false);
                
                String query = "INSERT OR REPLACE INTO arenas (id, name, world, pos1_x, pos1_y, pos1_z, pos2_x, pos2_y, pos2_z, enabled) " +
                               "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
                
                try (PreparedStatement ps = connection.prepareStatement(query)) {
                    for (Arena arena : arenas.values()) {
                        ps.setString(1, arena.getId());
                        ps.setString(2, arena.getName());
                        ps.setString(3, arena.getWorldName());
                        
                        Location pos1 = arena.getPos1();
                        ps.setDouble(4, pos1.getX());
                        ps.setDouble(5, pos1.getY());
                        ps.setDouble(6, pos1.getZ());
                        
                        Location pos2 = arena.getPos2();
                        ps.setDouble(7, pos2.getX());
                        ps.setDouble(8, pos2.getY());
                        ps.setDouble(9, pos2.getZ());
                        
                        ps.setInt(10, arena.isEnabled() ? 1 : 0);
                        
                        ps.addBatch();
                    }
                    
                    ps.executeBatch();
                    connection.commit();
                    
                    future.complete(true);
                }
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Arenalar toplu kaydedilirken hata oluştu!", e);
                
                try {
                    if (connection != null) {
                        connection.rollback();
                    }
                } catch (SQLException rollbackEx) {
                    plugin.getLogger().log(Level.SEVERE, "Veritabanı geri alma işlemi başarısız oldu!", rollbackEx);
                }
                
                future.complete(false);
            } finally {
                try {
                    if (connection != null) {
                        connection.setAutoCommit(true);
                    }
                } catch (SQLException autoCommitEx) {
                    plugin.getLogger().log(Level.SEVERE, "Auto-commit ayarlanırken hata oluştu!", autoCommitEx);
                }
            }
        });
        
        return future;
    }

    /**
     * Arenayı veritabanından siler
     * 
     * @param arenaId Arena ID'si
     * @return İşlem başarılı olursa true döner
     */
    public CompletableFuture<Boolean> deleteArena(String arenaId) {
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                initDatabase(); // Bağlantıyı kontrol et
                
                String query = "DELETE FROM arenas WHERE id = ?";
                
                try (PreparedStatement ps = connection.prepareStatement(query)) {
                    ps.setString(1, arenaId);
                    
                    int affectedRows = ps.executeUpdate();
                    future.complete(affectedRows > 0);
                }
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Arena silinirken hata oluştu: " + arenaId, e);
                future.complete(false);
            }
        });
        
        return future;
    }
    
    /**
     * Oyuncu istatistiklerini veritabanına kaydeder
     * 
     * @param playerStats Oyuncu istatistikleri
     * @param playerName Oyuncu adı
     * @return İşlem başarılı olursa true döner
     */
    public CompletableFuture<Boolean> savePlayerStats(PlayerStats playerStats, String playerName) {
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                initDatabase(); // Bağlantıyı kontrol et
                
                String query = "INSERT OR REPLACE INTO player_stats " +
                               "(uuid, player_name, wins, losses, money_won, money_lost, last_updated) " +
                               "VALUES (?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP)";
                
                try (PreparedStatement ps = connection.prepareStatement(query)) {
                    ps.setString(1, playerStats.getPlayerUuid().toString());
                    ps.setString(2, playerName);
                    ps.setInt(3, playerStats.getWins());
                    ps.setInt(4, playerStats.getLosses());
                    ps.setDouble(5, playerStats.getMoneyWon());
                    ps.setDouble(6, playerStats.getMoneyLost());
                    
                    ps.executeUpdate();
                    
                    // Debug log
                    if (plugin.getConfig().getBoolean("debug", false)) {
                        plugin.getLogger().info("Oyuncu istatistikleri kaydedildi: " + playerName);
                    }
                    
                    future.complete(true);
                }
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Oyuncu istatistikleri kaydedilirken hata oluştu: " + playerStats.getPlayerUuid(), e);
                future.complete(false);
            }
        });
        
        return future;
    }
    
    /**
     * Oyuncu istatistiklerini veritabanından yükler
     * 
     * @param uuid Oyuncu UUID'si
     * @return Oyuncu istatistikleri, bulunamazsa yeni bir nesne döner
     */
    public CompletableFuture<PlayerStats> loadPlayerStats(UUID uuid) {
        CompletableFuture<PlayerStats> future = new CompletableFuture<>();
        
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                initDatabase(); // Bağlantıyı kontrol et
                
                String query = "SELECT * FROM player_stats WHERE uuid = ?";
                
                try (PreparedStatement ps = connection.prepareStatement(query)) {
                    ps.setString(1, uuid.toString());
                    
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            int wins = rs.getInt("wins");
                            int losses = rs.getInt("losses");
                            double moneyWon = rs.getDouble("money_won");
                            double moneyLost = rs.getDouble("money_lost");
                            
                            PlayerStats stats = new PlayerStats(uuid, wins, losses);
                            stats.addMoneyWon(moneyWon);
                            stats.addMoneyLost(moneyLost);
                            
                            future.complete(stats);
                        } else {
                            // Oyuncu veritabanında bulunamadı, yeni istatistik nesnesi döndür
                            PlayerStats newStats = new PlayerStats(uuid);
                            future.complete(newStats);
                        }
                    }
                }
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Oyuncu istatistikleri yüklenirken hata oluştu: " + uuid, e);
                future.complete(new PlayerStats(uuid)); // Hata durumunda yeni nesne döndür
            }
        });
        
        return future;
    }
    
    /**
     * En iyi oyuncuları sıralamaya göre getirir
     * 
     * @param limit Maksimum oyuncu sayısı
     * @param orderBy Sıralama kriteri (wins, money_won, win_ratio)
     * @return İstatistikler ve oyuncu adları haritası
     */
    public CompletableFuture<List<Map.Entry<PlayerStats, String>>> getTopPlayers(int limit, String orderBy) {
        CompletableFuture<List<Map.Entry<PlayerStats, String>>> future = new CompletableFuture<>();
        
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            List<Map.Entry<PlayerStats, String>> result = new ArrayList<>();
            
            try {
                initDatabase(); // Bağlantıyı kontrol et
                
                String orderClause;
                switch (orderBy.toLowerCase()) {
                    case "wins":
                        orderClause = "wins DESC";
                        break;
                    case "money_won":
                        orderClause = "money_won DESC";
                        break;
                    case "win_ratio":
                        orderClause = "(CASE WHEN (wins + losses) > 0 THEN CAST(wins AS REAL) / (wins + losses) ELSE 0 END) DESC";
                        break;
                    default:
                        orderClause = "wins DESC";
                        break;
                }
                
                String query = "SELECT * FROM player_stats ORDER BY " + orderClause + " LIMIT ?";
                
                try (PreparedStatement ps = connection.prepareStatement(query)) {
                    ps.setInt(1, limit);
                    
                    try (ResultSet rs = ps.executeQuery()) {
                        while (rs.next()) {
                            UUID uuid = UUID.fromString(rs.getString("uuid"));
                            String playerName = rs.getString("player_name");
                            int wins = rs.getInt("wins");
                            int losses = rs.getInt("losses");
                            double moneyWon = rs.getDouble("money_won");
                            double moneyLost = rs.getDouble("money_lost");
                            
                            PlayerStats stats = new PlayerStats(uuid, wins, losses);
                            stats.addMoneyWon(moneyWon);
                            stats.addMoneyLost(moneyLost);
                            
                            result.add(new AbstractMap.SimpleEntry<>(stats, playerName));
                        }
                    }
                }
                
                future.complete(result);
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "En iyi oyuncular listesi alınırken hata oluştu!", e);
                future.complete(Collections.emptyList());
            }
        });
        
        return future;
    }
    
    /**
     * Belirli bir sıralamadaki oyuncuyu getirir
     * 
     * @param position Sıralama pozisyonu (1'den başlar)
     * @param orderBy Sıralama kriteri (wins, money_won, win_ratio)
     * @return İstatistikler ve oyuncu adı, bulunamazsa null
     */
    public CompletableFuture<Map.Entry<PlayerStats, String>> getPlayerAtRank(int position, String orderBy) {
        CompletableFuture<Map.Entry<PlayerStats, String>> future = new CompletableFuture<>();
        
        // Pozisyon 1'den başlar, veritabanı 0'dan başlar
        int index = Math.max(0, position - 1);
        
        // Tek bir oyuncu almak için getTopPlayers metodunu kullan
        getTopPlayers(position, orderBy).thenAccept(list -> {
            if (list.size() > index) {
                future.complete(list.get(index));
            } else {
                future.complete(null);
            }
        });
        
        return future;
    }
    
    /**
     * Tüm oyuncuların istatistiklerini veritabanından yükler
     * 
     * @return Oyuncu UUID'lerine göre istatistikler haritası
     */
    public CompletableFuture<Map<UUID, PlayerStats>> loadAllPlayerStats() {
        CompletableFuture<Map<UUID, PlayerStats>> future = new CompletableFuture<>();
        Map<UUID, PlayerStats> stats = new HashMap<>();
        
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                initDatabase(); // Bağlantıyı kontrol et
                
                String query = "SELECT * FROM player_stats";
                
                try (Statement statement = connection.createStatement();
                     ResultSet rs = statement.executeQuery(query)) {
                    
                    while (rs.next()) {
                        UUID uuid = UUID.fromString(rs.getString("uuid"));
                        int wins = rs.getInt("wins");
                        int losses = rs.getInt("losses");
                        double moneyWon = rs.getDouble("money_won");
                        double moneyLost = rs.getDouble("money_lost");
                        
                        PlayerStats playerStats = new PlayerStats(uuid, wins, losses);
                        playerStats.addMoneyWon(moneyWon);
                        playerStats.addMoneyLost(moneyLost);
                        
                        stats.put(uuid, playerStats);
                    }
                }
                
                future.complete(stats);
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Tüm oyuncu istatistikleri yüklenirken hata oluştu!", e);
                future.complete(stats); // Boş veya kısmi harita döndür
            }
        });
        
        return future;
    }
} 