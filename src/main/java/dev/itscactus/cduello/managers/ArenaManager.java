package dev.itscactus.cduello.managers;

import dev.itscactus.cduello.Main;
import dev.itscactus.cduello.models.Arena;
import dev.itscactus.cduello.utils.MessageManager;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Arena yönetim sınıfı
 */
public class ArenaManager {

    private final Main plugin;
    private final MessageManager messageManager;
    private final Map<String, Arena> arenas = new ConcurrentHashMap<>();
    private boolean enabled;
    private boolean isLoading = false;
    private boolean isSaving = false;

    public ArenaManager(Main plugin) {
        this.plugin = plugin;
        this.messageManager = plugin.getMessageManager();
        this.enabled = plugin.getConfig().getBoolean("duels.arenas.enabled", false);
        loadArenas();
    }

    /**
     * Arenaları yapılandırma dosyasından asenkron olarak yükler
     */
    public void loadArenas() {
        if (isLoading) {
            return;
        }
        
        isLoading = true;
        
        new BukkitRunnable() {
            @Override
            public void run() {
                try {
                    arenas.clear();
                    
                    ConfigurationSection arenasSection = plugin.getConfig().getConfigurationSection("duels.arenas.list");
                    
                    if (arenasSection == null) {
                        return;
                    }
                    
                    enabled = plugin.getConfig().getBoolean("duels.arenas.enabled", false);
                    
                    for (String arenaId : arenasSection.getKeys(false)) {
                        ConfigurationSection arenaSection = arenasSection.getConfigurationSection(arenaId);
                        
                        if (arenaSection == null) {
                            continue;
                        }
                        
                        String name = arenaSection.getString("name", arenaId);
                        String worldName = arenaSection.getString("world", "world");
                        
                        World world = Bukkit.getWorld(worldName);
                        
                        if (world == null) {
                            plugin.getLogger().warning("Arena için dünya bulunamadı: " + worldName);
                            continue;
                        }
                        
                        ConfigurationSection pos1Section = arenaSection.getConfigurationSection("pos1");
                        ConfigurationSection pos2Section = arenaSection.getConfigurationSection("pos2");
                        
                        if (pos1Section == null || pos2Section == null) {
                            plugin.getLogger().warning("Arena için konum bilgisi eksik: " + arenaId);
                            continue;
                        }
                        
                        double x1 = pos1Section.getDouble("x");
                        double y1 = pos1Section.getDouble("y");
                        double z1 = pos1Section.getDouble("z");
                        
                        double x2 = pos2Section.getDouble("x");
                        double y2 = pos2Section.getDouble("y");
                        double z2 = pos2Section.getDouble("z");
                        
                        Location pos1 = new Location(world, x1, y1, z1);
                        Location pos2 = new Location(world, x2, y2, z2);
                        
                        Arena arena = new Arena(arenaId, name, world, pos1, pos2);
                        
                        boolean arenaEnabled = arenaSection.getBoolean("enabled", true);
                        arena.setEnabled(arenaEnabled);
                        
                        arenas.put(arenaId, arena);
                    }
                } finally {
                    isLoading = false;
                }
            }
        }.runTaskAsynchronously(plugin);
    }

    /**
     * Arenaları yapılandırma dosyasına asenkron olarak kaydeder
     * 
     * @return Tamamlandığında bildirecek CompletableFuture
     */
    public CompletableFuture<Void> saveArenas() {
        if (isSaving) {
            CompletableFuture<Void> future = new CompletableFuture<>();
            future.complete(null);
            return future;
        }
        
        isSaving = true;
        CompletableFuture<Void> future = new CompletableFuture<>();
        
        new BukkitRunnable() {
            @Override
            public void run() {
                try {
                    ConfigurationSection arenasSection = plugin.getConfig().createSection("duels.arenas.list");
                    
                    plugin.getConfig().set("duels.arenas.enabled", enabled);
                    
                    for (Arena arena : arenas.values()) {
                        ConfigurationSection arenaSection = arenasSection.createSection(arena.getId());
                        
                        arenaSection.set("name", arena.getName());
                        arenaSection.set("world", arena.getWorldName());
                        
                        Location pos1 = arena.getPos1();
                        ConfigurationSection pos1Section = arenaSection.createSection("pos1");
                        pos1Section.set("x", pos1.getX());
                        pos1Section.set("y", pos1.getY());
                        pos1Section.set("z", pos1.getZ());
                        
                        Location pos2 = arena.getPos2();
                        ConfigurationSection pos2Section = arenaSection.createSection("pos2");
                        pos2Section.set("x", pos2.getX());
                        pos2Section.set("y", pos2.getY());
                        pos2Section.set("z", pos2.getZ());
                    }
                } finally {
                    isSaving = false;
                }
            }
        }.runTaskAsynchronously(plugin);
        
        return future;
    }

    /**
     * Yeni bir arena oluşturur
     *
     * @param arenaId Arena ID'si
     * @param name Arena adı
     * @param world Arena dünyası
     * @param pos1 Birinci konum
     * @param pos2 İkinci konum
     * @return Başarılıysa true, değilse false
     */
    public boolean createArena(String arenaId, String name, World world, Location pos1, Location pos2) {
        if (arenas.containsKey(arenaId)) {
            return false;
        }

        Arena arena = new Arena(arenaId, name, world, pos1, pos2);
        arenas.put(arenaId, arena);
        saveArenas();
        return true;
    }

    /**
     * Bir arenayı siler
     *
     * @param arenaId Arena ID'si
     * @return Başarılıysa true, değilse false
     */
    public boolean deleteArena(String arenaId) {
        if (!arenas.containsKey(arenaId)) {
            return false;
        }

        arenas.remove(arenaId);
        saveArenas();
        return true;
    }

    /**
     * Bir arenanın adını değiştirir
     *
     * @param arenaId Arena ID'si
     * @param newName Yeni arena adı
     * @return Başarılıysa true, değilse false
     */
    public boolean renameArena(String arenaId, String newName) {
        Arena arena = arenas.get(arenaId);
        if (arena == null) {
            return false;
        }

        arena.setName(newName);
        saveArenas();
        return true;
    }

    /**
     * Bir arenanın birinci konumunu günceller
     *
     * @param arenaId Arena ID'si
     * @param location Yeni konum
     * @return Başarılıysa true, değilse false
     */
    public boolean updatePos1(String arenaId, Location location) {
        Arena arena = arenas.get(arenaId);
        if (arena == null) {
            return false;
        }

        arena.setPos1(location);
        saveArenas();
        return true;
    }

    /**
     * Bir arenanın ikinci konumunu günceller
     *
     * @param arenaId Arena ID'si
     * @param location Yeni konum
     * @return Başarılıysa true, değilse false
     */
    public boolean updatePos2(String arenaId, Location location) {
        Arena arena = arenas.get(arenaId);
        if (arena == null) {
            return false;
        }

        arena.setPos2(location);
        saveArenas();
        return true;
    }

    /**
     * Arenaları listeler
     *
     * @param player Oyuncu
     */
    public void listArenas(Player player) {
        messageManager.sendMessage(player, "arena-list-header");
        
        if (arenas.isEmpty()) {
            messageManager.sendMessage(player, "arena-list-empty");
            return;
        }

        for (Arena arena : arenas.values()) {
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("id", arena.getId());
            placeholders.put("name", arena.getName());
            placeholders.put("world", arena.getWorldName());
            placeholders.put("status", arena.isEnabled() ? "Etkin" : "Devre Dışı");
            
            messageManager.sendMessage(player, "arena-list-item", placeholders);
        }
        
        messageManager.sendMessage(player, "arena-list-footer");
    }

    /**
     * Arena bilgilerini gösterir
     *
     * @param player Oyuncu
     * @param arenaId Arena ID'si
     */
    public void showArenaInfo(Player player, String arenaId) {
        Arena arena = arenas.get(arenaId);
        if (arena == null) {
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("arena", arenaId);
            messageManager.sendMessage(player, "arena-not-found", placeholders);
            return;
        }

        Location pos1 = arena.getPos1();
        Location pos2 = arena.getPos2();
        
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("id", arena.getId());
        placeholders.put("name", arena.getName());
        placeholders.put("world", arena.getWorldName());
        placeholders.put("pos1", String.format("%.1f, %.1f, %.1f", pos1.getX(), pos1.getY(), pos1.getZ()));
        placeholders.put("pos2", String.format("%.1f, %.1f, %.1f", pos2.getX(), pos2.getY(), pos2.getZ()));
        placeholders.put("status", arena.isEnabled() ? "Etkin" : "Devre Dışı");
        
        messageManager.sendMessage(player, "arena-info-header", placeholders);
        messageManager.sendMessage(player, "arena-info-pos", placeholders);
        messageManager.sendMessage(player, "arena-info-status", placeholders);
        messageManager.sendMessage(player, "arena-info-footer");
    }

    /**
     * Bir arenanın etkinlik durumunu değiştirir
     *
     * @param arenaId Arena ID'si
     * @param enabled Etkin olup olmadığı
     * @return Başarılıysa true, değilse false
     */
    public boolean toggleArena(String arenaId, boolean enabled) {
        Arena arena = arenas.get(arenaId);
        if (arena == null) {
            return false;
        }

        arena.setEnabled(enabled);
        saveArenas();
        return true;
    }

    /**
     * Arena sisteminin etkinlik durumunu ayarlar
     *
     * @param enabled Etkin olup olmadığı
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        plugin.getConfig().set("duels.arenas.enabled", enabled);
        plugin.saveConfig();
    }

    /**
     * Arena sisteminin etkin olup olmadığını kontrol eder
     *
     * @return Etkinse true, değilse false
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Tüm arenaları döndürür
     *
     * @return Arena haritası
     */
    public Map<String, Arena> getArenas() {
        return arenas;
    }

    /**
     * Bir arenayı ID'sine göre alır
     *
     * @param arenaId Arena ID'si
     * @return Arena, bulunamazsa null
     */
    public Arena getArena(String arenaId) {
        return arenas.get(arenaId);
    }

    /**
     * Arena ID'lerini döndürür
     *
     * @return Arena ID'leri kümesi
     */
    public Set<String> getArenaIds() {
        return arenas.keySet();
    }
} 