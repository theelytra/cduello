package dev.itscactus.cduello.commands;

import dev.itscactus.cduello.Main;
import dev.itscactus.cduello.managers.ArenaManager;
import dev.itscactus.cduello.managers.DuelManager;
import dev.itscactus.cduello.managers.EconomyManager;
import dev.itscactus.cduello.utils.LeaderboardGUI;
import dev.itscactus.cduello.utils.MessageManager;
import dev.itscactus.cduello.utils.MessageUtils;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.Location;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class DuelloCommand implements CommandExecutor, TabCompleter {

    private final Main plugin;
    private final DuelManager duelManager;
    private final EconomyManager economyManager;
    private final MessageManager messageManager;
    private final ArenaManager arenaManager;
    
    // Position selectors for arena creation
    private final Map<UUID, Location> pos1Selectors = new ConcurrentHashMap<>();
    private final Map<UUID, Location> pos2Selectors = new ConcurrentHashMap<>();
    
    // Önceden hazırlanmış listeler (performans için)
    private final List<String> SUBCOMMANDS = Arrays.asList("kabul", "reddet", "istatistik", "yardım");
    private final List<String> ADMIN_SUBCOMMANDS = Arrays.asList("kabul", "reddet", "istatistik", "yardım", "admin", "arena");
    private final List<String> ADMIN_COMMANDS = Arrays.asList("reload");
    private final List<String> ARENA_SUBCOMMANDS = Arrays.asList("liste", "oluştur", "sil", "yenidenadlandır", "bilgi", "pos1", "pos2", "etkinleştir", "devreDışıBırak");
    private final List<String> EMPTY_LIST = Collections.emptyList();

    /**
     * DuelloCommand sınıfını oluşturur
     *
     * @param plugin Ana eklenti örneği
     * @param duelManager Düello yöneticisi
     */
    public DuelloCommand(Main plugin, DuelManager duelManager) {
        this.plugin = plugin;
        this.duelManager = duelManager;
        this.economyManager = plugin.getEconomyManager();
        this.messageManager = plugin.getMessageManager();
        this.arenaManager = plugin.getArenaManager();
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

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "accept":
            case "kabul":
                handleAccept(player);
                break;
            case "deny":
            case "reddet":
                handleDeny(player);
                break;
            case "stats":
            case "istatistik":
                handleStats(player, args);
                break;
            case "reload":
                handleAdminCommand(player, args);
                break;
            case "arena":
                handleArenaCommand(player, args);
                break;
            case "admin":
                handleAdminCommand(player, args);
                break;
            case "sıralama":
            case "top":
                openLeaderboard(player);
                break;
            default:
                // Diğer tüm durumlarda oyuncu adı olarak kabul et ve düello isteği gönder
                Player target = Bukkit.getPlayerExact(subCommand);
                handleDuelRequest(player, args);
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

    /**
     * Arena komutlarını işler
     * 
     * @param player Komutu kullanan oyuncu
     * @param args Komut argümanları
     */
    private void handleArenaCommand(Player player, String[] args) {
        // Admin yetkisi kontrolü
        if (!player.hasPermission("cduello.admin")) {
            messageManager.sendMessage(player, "no-permission");
            return;
        }
        
        // Alt komut yoksa arena listesini göster
        if (args.length < 2) {
            arenaManager.listArenas(player);
            return;
        }
        
        String arenaCommand = args[1].toLowerCase();
        
        switch (arenaCommand) {
            case "list":
            case "liste":
                arenaManager.listArenas(player);
                break;
                
            case "create":
            case "oluştur":
            case "olustur":
                if (args.length < 4) {
                    messageManager.sendMessage(player, "arena-create-usage");
                    return;
                }
                
                String arenaId = args[2].toLowerCase();
                String arenaName = args[3];
                
                Location pos1 = pos1Selectors.get(player.getUniqueId());
                Location pos2 = pos2Selectors.get(player.getUniqueId());
                
                if (pos1 == null || pos2 == null) {
                    messageManager.sendMessage(player, "arena-positions-not-set");
                    return;
                }
                
                boolean created = arenaManager.createArena(arenaId, arenaName, player.getWorld(), pos1, pos2);
                
                if (created) {
                    Map<String, String> placeholders = new HashMap<>();
                    placeholders.put("arena", arenaName);
                    placeholders.put("id", arenaId);
                    messageManager.sendMessage(player, "arena-created", placeholders);
                } else {
                    Map<String, String> placeholders = new HashMap<>();
                    placeholders.put("arena", arenaId);
                    messageManager.sendMessage(player, "arena-already-exists", placeholders);
                }
                break;
                
            case "delete":
            case "sil":
                if (args.length < 3) {
                    messageManager.sendMessage(player, "arena-delete-usage");
                    return;
                }
                
                arenaId = args[2].toLowerCase();
                
                boolean deleted = arenaManager.deleteArena(arenaId);
                
                if (deleted) {
                    Map<String, String> placeholders = new HashMap<>();
                    placeholders.put("arena", arenaId);
                    messageManager.sendMessage(player, "arena-deleted", placeholders);
                } else {
                    Map<String, String> placeholders = new HashMap<>();
                    placeholders.put("arena", arenaId);
                    messageManager.sendMessage(player, "arena-not-found", placeholders);
                }
                break;
                
            case "rename":
            case "yenidenadlandir":
            case "yenidenadlandır":
                if (args.length < 4) {
                    messageManager.sendMessage(player, "arena-rename-usage");
                    return;
                }
                
                arenaId = args[2].toLowerCase();
                String newName = args[3];
                
                boolean renamed = arenaManager.renameArena(arenaId, newName);
                
                if (renamed) {
                    Map<String, String> placeholders = new HashMap<>();
                    placeholders.put("arena", arenaId);
                    placeholders.put("name", newName);
                    messageManager.sendMessage(player, "arena-renamed", placeholders);
                } else {
                    Map<String, String> placeholders = new HashMap<>();
                    placeholders.put("arena", arenaId);
                    messageManager.sendMessage(player, "arena-not-found", placeholders);
                }
                break;
                
            case "info":
            case "bilgi":
                if (args.length < 3) {
                    messageManager.sendMessage(player, "arena-info-usage");
                    return;
                }
                
                arenaId = args[2].toLowerCase();
                arenaManager.showArenaInfo(player, arenaId);
                break;
                
            case "pos1":
                pos1Selectors.put(player.getUniqueId(), player.getLocation().clone());
                messageManager.sendMessage(player, "arena-pos1-set");
                break;
                
            case "pos2":
                pos2Selectors.put(player.getUniqueId(), player.getLocation().clone());
                messageManager.sendMessage(player, "arena-pos2-set");
                break;
                
            case "enable":
            case "etkinlestir":
            case "etkinleştir":
                if (args.length < 3) {
                    messageManager.sendMessage(player, "arena-toggle-usage");
                    return;
                }
                
                arenaId = args[2].toLowerCase();
                boolean enabled = arenaManager.toggleArena(arenaId, true);
                
                if (enabled) {
                    Map<String, String> placeholders = new HashMap<>();
                    placeholders.put("arena", arenaId);
                    messageManager.sendMessage(player, "arena-enabled", placeholders);
                } else {
                    Map<String, String> placeholders = new HashMap<>();
                    placeholders.put("arena", arenaId);
                    messageManager.sendMessage(player, "arena-not-found", placeholders);
                }
                break;
                
            case "disable":
            case "devredışıbırak":
            case "devredisibirak":
                if (args.length < 3) {
                    messageManager.sendMessage(player, "arena-toggle-usage");
                    return;
                }
                
                arenaId = args[2].toLowerCase();
                boolean disabled = arenaManager.toggleArena(arenaId, false);
                
                if (disabled) {
                    Map<String, String> placeholders = new HashMap<>();
                    placeholders.put("arena", arenaId);
                    messageManager.sendMessage(player, "arena-disabled", placeholders);
                } else {
                    Map<String, String> placeholders = new HashMap<>();
                    placeholders.put("arena", arenaId);
                    messageManager.sendMessage(player, "arena-not-found", placeholders);
                }
                break;
                
            default:
                arenaManager.listArenas(player);
                break;
        }
    }

    /**
     * Admin komutlarını işler
     * 
     * @param player Komutu kullanan oyuncu
     * @param args Komut argümanları
     */
    private void handleAdminCommand(Player player, String[] args) {
        // Admin permission check
        if (!player.hasPermission("cduello.admin")) {
            messageManager.sendMessage(player, "no-permission");
            return;
        }
        
        // Show help message if no subcommand is provided
        if (args.length < 2) {
            messageManager.sendMessage(player, "admin-help");
            return;
        }
        
        String adminCommand = args[1].toLowerCase();
        
        switch (adminCommand) {
            case "reload":
                plugin.reloadConfig();
                messageManager.sendMessage(player, "config-reloaded");
                break;
            
            default:
                messageManager.sendMessage(player, "admin-help");
                break;
        }
    }

    /**
     * Düello isteklerini işler
     * 
     * @param player Komutu kullanan oyuncu
     * @param args Komut argümanları
     */
    private void handleDuelRequest(Player player, String[] args) {
        String targetName = args[0];
        Player target = Bukkit.getPlayer(targetName);
        
        if (target == null) {
            messageManager.sendMessage(player, "player-not-found");
            return;
        }
        
        if (target.getUniqueId().equals(player.getUniqueId())) {
            messageManager.sendMessage(player, "no-self-duel");
            return;
        }
        
        // Bu bir para düellosu mu kontrol et
        if (args.length >= 2 && economyManager.isEconomyEnabled()) {
            try {
                double amount = Double.parseDouble(args[1]);
                handleMoneyDuel(player, target, amount);
            } catch (NumberFormatException e) {
                messageManager.sendMessage(player, "invalid-amount");
            }
        } else {
            // Normal düello
            duelManager.sendDuelRequest(player, target);
        }
    }

    /**
     * Sıralama menüsünü açar
     * 
     * @param player Oyuncu
     */
    private void openLeaderboard(Player player) {
        // Varsayılan olarak galibiyet sıralaması göster
        new LeaderboardGUI(plugin, plugin.getDatabaseManager(), "wins").open(player);
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
            String input = args[0].toLowerCase();
            List<String> completions = new ArrayList<>();
            
            // Add command suggestions
            List<String> subCommands = player.hasPermission("cduello.admin") ? ADMIN_SUBCOMMANDS : SUBCOMMANDS;
            for (String cmd : subCommands) {
                if (cmd.toLowerCase().startsWith(input)) {
                    completions.add(cmd);
                }
            }
            
            // Add player name suggestions
            for (Player p : Bukkit.getOnlinePlayers()) {
                if (p.getUniqueId().equals(player.getUniqueId())) {
                    continue; // Skip the sender
                }
                
                if (p.getName().toLowerCase().startsWith(input)) {
                    completions.add(p.getName());
                }
            }
            
            return completions;
        } else if (args.length == 2) {
            String subCommand = args[0].toLowerCase();
            
            if (subCommand.equals("istatistik") || subCommand.equals("stats")) {
                return filterCompletions(getOnlinePlayerNames(), args[1]);
            } else if (subCommand.equals("admin") && player.hasPermission("cduello.admin")) {
                return filterCompletions(ADMIN_COMMANDS, args[1]);
            } else if (subCommand.equals("arena") && player.hasPermission("cduello.admin")) {
                return filterCompletions(ARENA_SUBCOMMANDS, args[1]);
            }
        } else if (args.length == 3 && args[0].toLowerCase().equals("arena")) {
            String arenaSubCommand = args[1].toLowerCase();
            
            if (arenaSubCommand.equals("sil") || 
                arenaSubCommand.equals("yenidenadlandır") || 
                arenaSubCommand.equals("bilgi") ||
                arenaSubCommand.equals("etkinleştir") ||
                arenaSubCommand.equals("devredışıbırak")) {
                
                return filterCompletions(new ArrayList<>(arenaManager.getArenaIds()), args[2]);
            }
        }
        
        return EMPTY_LIST;
    }

    private List<String> getOnlinePlayerNames() {
        return Bukkit.getOnlinePlayers().stream()
                .map(Player::getName)
                .collect(Collectors.toList());
    }

    private List<String> filterCompletions(List<String> completions, String input) {
        if (completions == null) {
            return new ArrayList<>();
        }
        
        if (input == null || input.isEmpty()) {
            return new ArrayList<>(completions);
        }
        
        String lowerInput = input.toLowerCase();
        
        return completions.stream()
                .filter(c -> c != null && c.toLowerCase().startsWith(lowerInput))
                .collect(Collectors.toList());
    }
} 