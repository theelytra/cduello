package dev.itscactus.cduello.utils;

import dev.itscactus.cduello.Main;
import dev.itscactus.cduello.models.PlayerStats;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Sıralama menüsü GUI sınıfı
 */
public class LeaderboardGUI {
    private final Main plugin;
    private final DatabaseManager databaseManager;
    private final String orderType;
    private final DecimalFormat decimalFormat = new DecimalFormat("#,##0.00");
    private final DecimalFormat percentFormat = new DecimalFormat("#0.0%");

    /**
     * GUI yapıcı
     * 
     * @param plugin Plugin
     * @param databaseManager Veritabanı yöneticisi
     * @param orderType Sıralama tipi (wins, money_won, win_ratio)
     */
    public LeaderboardGUI(Main plugin, DatabaseManager databaseManager, String orderType) {
        this.plugin = plugin;
        this.databaseManager = databaseManager;
        this.orderType = orderType;
    }

    /**
     * En iyi oyuncular menüsünü oyuncuya açar
     * 
     * @param player Oyuncu
     */
    public void open(Player player) {
        // Asenkron olarak verileri çek
        databaseManager.getTopPlayers(10, orderType).thenAccept(topPlayers -> {
            // Ana thread'e geri dön
            Bukkit.getScheduler().runTask(plugin, () -> {
                // Eğer oyuncu çevrimiçi değilse
                if (!player.isOnline()) {
                    return;
                }
                
                // GUI'yi oluştur
                Inventory gui = createInventory(topPlayers);
                
                // Oyuncuya aç
                player.openInventory(gui);
            });
        });
    }

    /**
     * Envanteri oluşturur
     * 
     * @param topPlayers En iyi oyuncular listesi
     * @return Oluşturulan envanter
     */
    private Inventory createInventory(List<Map.Entry<PlayerStats, String>> topPlayers) {
        // Başlık belirle
        String title;
        switch (orderType.toLowerCase()) {
            case "wins":
                title = ChatColor.GOLD + "En Çok Galibiyet";
                break;
            case "money_won":
                title = ChatColor.GOLD + "En Çok Para Kazananlar";
                break;
            case "win_ratio":
                title = ChatColor.GOLD + "En İyi Oran";
                break;
            default:
                title = ChatColor.GOLD + "Düello Sıralaması";
                break;
        }
        
        // 9x3 envanter oluştur (27 slot)
        Inventory inventory = Bukkit.createInventory(null, 27, title);
        
        // Arkaplan olarak siyah cam panelleri kullan
        ItemStack backgroundItem = new ItemStack(Material.BLACK_STAINED_GLASS_PANE);
        ItemMeta backgroundMeta = backgroundItem.getItemMeta();
        backgroundMeta.setDisplayName(" ");
        backgroundItem.setItemMeta(backgroundMeta);
        
        // Tüm slotları siyah camla doldur
        for (int i = 0; i < 27; i++) {
            inventory.setItem(i, backgroundItem);
        }
        
        // Sıralama pozisyonları (envanter içinde)
        int[] positions = {13, 12, 14, 11, 15, 10, 16, 9, 17, 18};
        
        // En iyi 10 oyuncuyu ekle
        for (int i = 0; i < Math.min(topPlayers.size(), 10); i++) {
            Map.Entry<PlayerStats, String> entry = topPlayers.get(i);
            PlayerStats stats = entry.getKey();
            String playerName = entry.getValue();
            
            // İlgili slotu hesapla
            int slot = positions[i];
            
            // Oyuncu başı öğesini ekle
            inventory.setItem(slot, createPlayerHead(i + 1, stats, playerName));
        }
        
        // Filtre butonlarını ekle
        inventory.setItem(22, createFilterItem(Material.DIAMOND_SWORD, "Galibiyet Sıralaması", "wins"));
        inventory.setItem(23, createFilterItem(Material.GOLD_INGOT, "Para Sıralaması", "money_won"));
        inventory.setItem(24, createFilterItem(Material.EXPERIENCE_BOTTLE, "Oran Sıralaması", "win_ratio"));
        
        return inventory;
    }

    /**
     * Oyuncu başı öğesi oluşturur
     * 
     * @param position Sıralama pozisyonu
     * @param stats Oyuncu istatistikleri
     * @param playerName Oyuncu adı
     * @return Oluşturulan öğe
     */
    private ItemStack createPlayerHead(int position, PlayerStats stats, String playerName) {
        // Oyuncu başı item'ı oluştur
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) head.getItemMeta();
        
        // Oyuncuyu bul ve başı ayarla
        OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(stats.getPlayerUuid());
        meta.setOwningPlayer(offlinePlayer);
        
        // Meta'yı ayarla
        String positionColor;
        if (position == 1) {
            positionColor = ChatColor.GOLD.toString();
        } else if (position == 2) {
            positionColor = ChatColor.GRAY.toString();
        } else if (position == 3) {
            positionColor = ChatColor.DARK_RED.toString();
        } else {
            positionColor = ChatColor.DARK_GRAY.toString();
        }
        
        meta.setDisplayName(positionColor + "#" + position + " " + ChatColor.YELLOW + playerName);
        
        // Açıklamaları ekle
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.WHITE + "Galibiyetler: " + ChatColor.GREEN + stats.getWins());
        lore.add(ChatColor.WHITE + "Mağlubiyetler: " + ChatColor.RED + stats.getLosses());
        lore.add(ChatColor.WHITE + "Toplam Düello: " + ChatColor.AQUA + stats.getTotalDuels());
        
        double winRatio = stats.getWinRatio();
        String winRatioColor = winRatio >= 0.7 ? ChatColor.GREEN.toString() : 
                              winRatio >= 0.4 ? ChatColor.YELLOW.toString() : ChatColor.RED.toString();
        lore.add(ChatColor.WHITE + "Kazanma Oranı: " + winRatioColor + percentFormat.format(winRatio));
        
        lore.add("");
        lore.add(ChatColor.WHITE + "Kazanılan Para: " + ChatColor.GOLD + decimalFormat.format(stats.getMoneyWon()));
        lore.add(ChatColor.WHITE + "Kaybedilen Para: " + ChatColor.RED + decimalFormat.format(stats.getMoneyLost()));
        
        meta.setLore(lore);
        head.setItemMeta(meta);
        
        return head;
    }

    /**
     * Filtre butonu öğesi oluşturur
     * 
     * @param material Materyal
     * @param displayName Görünen isim
     * @param filterType Filtre tipi
     * @return Oluşturulan öğe
     */
    private ItemStack createFilterItem(Material material, String displayName, String filterType) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        
        // Aktif filtre ise vurgula
        if (orderType.equalsIgnoreCase(filterType)) {
            meta.setDisplayName(ChatColor.GREEN + "▶ " + displayName + " ◀");
        } else {
            meta.setDisplayName(ChatColor.YELLOW + displayName);
        }
        
        List<String> lore = new ArrayList<>();
        lore.add(ChatColor.GRAY + "Bu filtreyi uygulamak için tıkla");
        
        meta.setLore(lore);
        item.setItemMeta(meta);
        
        return item;
    }
} 