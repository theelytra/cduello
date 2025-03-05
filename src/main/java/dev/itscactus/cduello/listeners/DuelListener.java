package dev.itscactus.cduello.listeners;

import dev.itscactus.cduello.Main;
import dev.itscactus.cduello.managers.DuelManager;
import dev.itscactus.cduello.models.Duel;
import dev.itscactus.cduello.utils.MessageManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.Location;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Handles events related to duels
 */
public class DuelListener implements Listener {

    private final Main plugin;
    private final DuelManager duelManager;
    private final MessageManager messageManager;
    private final Map<UUID, Long> lastMovementWarning = new HashMap<>();
    private final Set<UUID> playersInCountdown = Collections.newSetFromMap(new ConcurrentHashMap<>());

    public DuelListener(Main plugin, DuelManager duelManager) {
        this.plugin = plugin;
        this.duelManager = duelManager;
        this.messageManager = plugin.getMessageManager();
    }

    /**
     * Handle player death during a duel
     */
    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        
        if (!duelManager.isInDuel(player.getUniqueId())) {
            return;
        }
        
        Duel duel = duelManager.getActiveDuel(player.getUniqueId());
        if (duel == null) {
            return;
        }
        
        if (plugin.getConfig().getBoolean("duels.keep-inventory", false)) {
            event.setKeepInventory(true);
            event.getDrops().clear();
            event.setKeepLevel(true);
            event.setDroppedExp(0);
        }
        
        // End the duel with the opponent as the winner
        duelManager.endDuel(duel, duel.getOpponent(player.getUniqueId()));
    }

    /**
     * Handle player quit during a duel
     */
    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        
        if (!duelManager.isInDuel(player.getUniqueId())) {
            return;
        }
        
        Duel duel = duelManager.getActiveDuel(player.getUniqueId());
        if (duel == null) {
            return;
        }
        
        // End the duel with the opponent as the winner
        duelManager.endDuel(duel, duel.getOpponent(player.getUniqueId()));
    }

    /**
     * Handle player damage during a duel
     */
    @EventHandler
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        if (!(event.getEntity() instanceof Player) || !(event.getDamager() instanceof Player)) {
            return;
        }
        
        Player damaged = (Player) event.getEntity();
        Player damager = (Player) event.getDamager();
        
        boolean damagedInDuel = duelManager.isInDuel(damaged.getUniqueId());
        boolean damagerInDuel = duelManager.isInDuel(damager.getUniqueId());
        
        // If either player is not in a duel
        if (!damagedInDuel || !damagerInDuel) {
            // If the damager is in a duel but the damaged is not, cancel
            if (damagerInDuel) {
                event.setCancelled(true);
            }
            // If the damaged is in a duel but the damager is not, cancel
            else if (damagedInDuel) {
                event.setCancelled(true);
            }
            return;
        }
        
        // Both players are in a duel, but check if they're in the same duel
        Duel damagedDuel = duelManager.getActiveDuel(damaged.getUniqueId());
        Duel damagerDuel = duelManager.getActiveDuel(damager.getUniqueId());
        
        if (damagedDuel != damagerDuel || !damagedDuel.hasPlayer(damager.getUniqueId())) {
            event.setCancelled(true);
        }
    }

    /**
     * Handle player commands during a duel
     */
    @EventHandler
    public void onPlayerCommand(PlayerCommandPreprocessEvent event) {
        Player player = event.getPlayer();
        
        if (!duelManager.isInDuel(player.getUniqueId()) || player.hasPermission("cduello.admin")) {
            return;
        }
        
        String command = event.getMessage().split(" ")[0].toLowerCase();
        
        // Allow duel command
        if (command.equalsIgnoreCase("/duel") || command.equalsIgnoreCase("/duello")) {
            return;
        }
        
        // Check if the command is in the allowed commands list
        boolean isAllowed = false;
        for (String allowedCommand : plugin.getConfig().getStringList("duels.allowed-commands")) {
            if (command.equalsIgnoreCase("/" + allowedCommand)) {
                isAllowed = true;
                break;
            }
        }
        
        if (!isAllowed) {
            event.setCancelled(true);
            player.sendMessage(plugin.getConfig().getString("messages.no-commands-in-duel", 
                    "&cYou cannot use commands during a duel!"));
        }
    }

    /**
     * Geri sayım sırasında oyuncuların hareket etmesini engeller
     * Bu event çok sık tetiklendiği için maksimum performans için optimize edilmiştir
     *
     * @param event Hareket olayı
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onPlayerMove(PlayerMoveEvent event) {
        // Hemen kontrol edelim ve erken çıkış imkanı verelim
        if (playersInCountdown.isEmpty()) {
            return;
        }
        
        Player player = event.getPlayer();
        UUID playerUuid = player.getUniqueId();
        
        // Set'te contains aramak Map'te arama yapmaktan daha hızlıdır
        if (!playersInCountdown.contains(playerUuid)) {
            return;
        }
        
        // Performans için sadece x ve z kontrolü yapıyoruz
        // Objeler oluşturmak yerine direkt değerleri karşılaştıralım
        double fromX = event.getFrom().getX();
        double fromZ = event.getFrom().getZ();
        double toX = event.getTo().getX();
        double toZ = event.getTo().getZ();
        
        // Çok küçük hareketleri yoksay (sadece kamera dönüşü olabilir)
        if (Math.abs(fromX - toX) < 0.01 && Math.abs(fromZ - toZ) < 0.01) {
            return;
        }
        
        // Hareket saptandı, oyuncuyu eski konumuna gönder
        Location loc = event.getFrom().clone();
        // Bakış açısını koru
        loc.setPitch(event.getTo().getPitch());
        loc.setYaw(event.getTo().getYaw());
        
        event.setTo(loc);
        
        // Mesajı 2 saniyede bir göster (spamı önle)
        long currentTime = System.currentTimeMillis();
        Long lastWarningTime = lastMovementWarning.get(playerUuid);
        
        if (lastWarningTime == null || currentTime - lastWarningTime > 2000) {
            messageManager.sendMessage(player, "duel-countdown-freeze");
            lastMovementWarning.put(playerUuid, currentTime);
        }
    }

    /**
     * Countdown set'ini güncellemek için
     */
    public void updateCountdownPlayers(Set<UUID> players) {
        playersInCountdown.clear();
        if (players != null && !players.isEmpty()) {
            playersInCountdown.addAll(players);
        }
    }
} 