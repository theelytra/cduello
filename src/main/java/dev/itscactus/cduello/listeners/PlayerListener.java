package dev.itscactus.cduello.listeners;

import dev.itscactus.cduello.Main;
import dev.itscactus.cduello.managers.DuelManager;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * Dinleyici sınıfı - Oyuncu event'lerini dinler
 */
public class PlayerListener implements Listener {

    private final Main plugin;
    private final DuelManager duelManager;

    public PlayerListener(Main plugin, DuelManager duelManager) {
        this.plugin = plugin;
        this.duelManager = duelManager;
    }

    /**
     * Oyuncu sunucuya katıldığında çalışır
     */
    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        // Oyuncu istatistiklerini yükle veya diğer giriş işlemleri
        // Buraya ihtiyaca göre daha fazla kod eklenebilir
    }

    /**
     * Oyuncu sunucudan ayrıldığında çalışır (DuelListener'daki işlemlerin yanında)
     */
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        // Oyuncu ayrıldığında istatistikleri kaydetme veya diğer işlemler
        // Buraya ihtiyaca göre daha fazla kod eklenebilir
    }
} 