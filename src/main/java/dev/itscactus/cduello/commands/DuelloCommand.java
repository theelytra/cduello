package dev.itscactus.cduello.commands;

import dev.itscactus.cduello.Main;
import dev.itscactus.cduello.managers.DuelManager;
import dev.itscactus.cduello.managers.EconomyManager;
import dev.itscactus.cduello.utils.MessageManager;
import dev.itscactus.cduello.utils.MessageUtils;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class DuelloCommand implements CommandExecutor, TabCompleter {

    private final Main plugin;
    private final DuelManager duelManager;
    private final EconomyManager economyManager;
    private final MessageManager messageManager;

    public DuelloCommand(Main plugin, DuelManager duelManager) {
        this.plugin = plugin;
        this.duelManager = duelManager;
        this.economyManager = plugin.getEconomyManager();
        this.messageManager = plugin.getMessageManager();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            messageManager.sendMessage(sender, "Bu komut sadece oyuncular tarafından kullanılabilir!");
            return true;
        }

        Player player = (Player) sender;

        if (!player.hasPermission("cduello.use")) {
            messageManager.sendMessage(player, "no-permission");
            return true;
        }

        if (args.length == 0) {
            sendHelp(player);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "accept":
                handleAccept(player);
                break;
            case "deny":
                handleDeny(player);
                break;
            case "stats":
                handleStats(player, args);
                break;
            case "reload":
                if (!player.hasPermission("cduello.admin")) {
                    messageManager.sendMessage(player, "no-permission");
                    return true;
                }
                plugin.reloadConfig();
                messageManager.sendMessage(player, plugin.getMessageManager().format(MessageUtils.getPrefix() + "<green>Yapılandırma yeniden yüklendi!"));
                break;
            default:
                // Eğer ilk argüman bir oyuncu ismi ise
                Player target = Bukkit.getPlayer(args[0]);
                if (target == null) {
                    messageManager.sendMessage(player, "player-not-found");
                    return true;
                }

                if (target.getUniqueId().equals(player.getUniqueId())) {
                    messageManager.sendMessage(player, "no-self-duel");
                    return true;
                }

                // Bu bir para düellosu mu kontrol et
                if (args.length >= 2 && economyManager.isEconomyEnabled()) {
                    try {
                        double amount = Double.parseDouble(args[1]);
                        handleMoneyDuel(player, target, amount);
                    } catch (NumberFormatException e) {
                        messageManager.sendMessage(player, plugin.getMessageManager().format(MessageUtils.getPrefix() + "<red>Geçersiz miktar! Kullanım: /" + label + " <oyuncu> <miktar>"));
                    }
                } else {
                    // Normal düello
                    duelManager.sendDuelRequest(player, target);
                }
                break;
        }

        return true;
    }

    private void handleAccept(Player player) {
        if (duelManager.isInDuel(player.getUniqueId())) {
            messageManager.sendMessage(player, "already-in-duel");
            return;
        }

        if (!duelManager.hasPendingRequests(player)) {
            messageManager.sendMessage(player, "no-pending-requests");
            return;
        }

        duelManager.acceptDuelRequest(player);
    }

    private void handleDeny(Player player) {
        if (!duelManager.hasPendingRequests(player)) {
            messageManager.sendMessage(player, "no-pending-requests");
            return;
        }

        duelManager.denyDuelRequest(player);
    }

    private void handleStats(Player player, String[] args) {
        if (args.length >= 2) {
            Player target = Bukkit.getPlayer(args[1]);
            if (target == null) {
                messageManager.sendMessage(player, "player-not-found");
                return;
            }
            duelManager.showStats(player, target);
        } else {
            duelManager.showStats(player, player);
        }
    }
    
    private void handleMoneyDuel(Player player, Player target, double amount) {
        if (!economyManager.isEconomyEnabled()) {
            messageManager.sendMessage(player, "economy-not-enabled");
            return;
        }
        
        // Miktar sınırlar içinde mi kontrol et
        if (amount < economyManager.getMinBetAmount()) {
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("amount", economyManager.formatMoney(economyManager.getMinBetAmount()));
            messageManager.sendMessage(player, "bet-too-low", placeholders);
            return;
        }
        
        if (amount > economyManager.getMaxBetAmount()) {
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("amount", economyManager.formatMoney(economyManager.getMaxBetAmount()));
            messageManager.sendMessage(player, "bet-too-high", placeholders);
            return;
        }
        
        duelManager.sendDuelRequest(player, target, amount);
    }

    private void sendHelp(Player player) {
        messageManager.sendMessage(player, "help-title");
        messageManager.sendMessage(player, "help-duel-player");
        
        if (economyManager != null && economyManager.isEconomyEnabled()) {
            messageManager.sendMessage(player, "help-duel-money");
        }
        
        messageManager.sendMessage(player, "help-duel-accept");
        messageManager.sendMessage(player, "help-duel-deny");
        messageManager.sendMessage(player, "help-duel-stats");
        
        if (player.hasPermission("cduello.admin")) {
            messageManager.sendMessage(player, "help-duel-reload");
        }
        
        messageManager.sendMessage(player, "help-footer");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!(sender instanceof Player)) {
            return new ArrayList<>();
        }

        Player player = (Player) sender;

        if (!player.hasPermission("cduello.use")) {
            return new ArrayList<>();
        }

        if (args.length == 1) {
            List<String> completions = new ArrayList<>(Arrays.asList("accept", "deny", "stats"));
            
            if (player.hasPermission("cduello.admin")) {
                completions.add("reload");
            }
            
            List<String> playerNames = Bukkit.getOnlinePlayers().stream()
                    .filter(p -> !p.getUniqueId().equals(player.getUniqueId()))
                    .map(Player::getName)
                    .collect(Collectors.toList());
            
            completions.addAll(playerNames);
            
            return completions.stream()
                    .filter(c -> c.toLowerCase().startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }

        if (args.length == 2) {
            if (args[0].equalsIgnoreCase("stats")) {
                return Bukkit.getOnlinePlayers().stream()
                        .map(Player::getName)
                        .filter(name -> name.toLowerCase().startsWith(args[1].toLowerCase()))
                        .collect(Collectors.toList());
            }
            
            // İlk argüman bir oyuncu ismi ve ekonomi etkinse, minimum bahis tutarını öner
            if (economyManager != null && economyManager.isEconomyEnabled() && Bukkit.getPlayer(args[0]) != null) {
                return List.of(String.valueOf((int)economyManager.getMinBetAmount()));
            }
        }

        return new ArrayList<>();
    }
} 