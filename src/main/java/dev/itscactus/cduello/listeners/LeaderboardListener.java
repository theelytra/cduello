package dev.itscactus.cduello.listeners;

import dev.itscactus.cduello.Main;
import dev.itscactus.cduello.utils.DatabaseManager;
import dev.itscactus.cduello.utils.LeaderboardGUI;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

/**
 * Sıralama menüsü için dinleyici
 */
public class LeaderboardListener implements Listener {
    private final Main plugin;
    private final DatabaseManager databaseManager;
    
    /**
     * Yapıcı metot
     * 
     * @param plugin Plugin ana sınıfı
     * @param databaseManager Veritabanı yöneticisi
     */
    public LeaderboardListener(Main plugin, DatabaseManager databaseManager) {
        this.plugin = plugin;
        this.databaseManager = databaseManager;
    }
    
    /**
     * Envanter tıklamalarını dinler
     * 
     * @param event Tıklama olayı
     */
    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        if (event.getView().getTitle() == null) {
            return;
        }
        
        String title = event.getView().getTitle();
        
        // Sıralama menüsü kontrolü
        if (title.contains("En Çok Galibiyet") || 
            title.contains("En Çok Para Kazananlar") || 
            title.contains("En İyi Oran") || 
            title.contains("Düello Sıralaması")) {
            
            event.setCancelled(true); // Klik etkileşimini iptal et
            
            if (event.getCurrentItem() == null || event.getCurrentItem().getType() == Material.AIR) {
                return;
            }
            
            Player player = (Player) event.getWhoClicked();
            ItemStack clickedItem = event.getCurrentItem();
            
            // Filtre butonları kontrolleri
            if (clickedItem.getType() == Material.DIAMOND_SWORD) {
                // Galibiyet sıralaması
                new LeaderboardGUI(plugin, databaseManager, "wins").open(player);
            } 
            else if (clickedItem.getType() == Material.GOLD_INGOT) {
                // Para sıralaması
                new LeaderboardGUI(plugin, databaseManager, "money_won").open(player);
            } 
            else if (clickedItem.getType() == Material.EXPERIENCE_BOTTLE) {
                // Oran sıralaması
                new LeaderboardGUI(plugin, databaseManager, "win_ratio").open(player);
            }
        }
    }
} 