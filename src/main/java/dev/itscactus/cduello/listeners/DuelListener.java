package dev.itscactus.cduello.listeners;

import dev.itscactus.cduello.Main;
import dev.itscactus.cduello.managers.DuelManager;
import dev.itscactus.cduello.models.Duel;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerQuitEvent;

/**
 * Handles events related to duels
 */
public class DuelListener implements Listener {

    private final Main plugin;
    private final DuelManager duelManager;

    public DuelListener(Main plugin, DuelManager duelManager) {
        this.plugin = plugin;
        this.duelManager = duelManager;
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
} 