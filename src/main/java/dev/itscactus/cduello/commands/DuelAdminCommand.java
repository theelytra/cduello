package dev.itscactus.cduello.commands;

import dev.itscactus.cduello.Main;
import dev.itscactus.cduello.managers.ArenaManager;
import dev.itscactus.cduello.models.Arena;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Düello yönetimi için admin komutları
 */
public class DuelAdminCommand implements CommandExecutor, TabCompleter {
    private final Main plugin;
    private final ArenaManager arenaManager;
    
    /**
     * Yapıcı metot
     * 
     * @param plugin Plugin ana sınıfı
     * @param arenaManager Arena yöneticisi
     */
    public DuelAdminCommand(Main plugin, ArenaManager arenaManager) {
        this.plugin = plugin;
        this.arenaManager = arenaManager;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Bu komutu sadece oyuncular kullanabilir.");
            return true;
        }
        
        Player player = (Player) sender;
        
        // İzin kontrolü
        if (!player.hasPermission("cduello.admin")) {
            plugin.getMessageManager().sendMessage(player, "no-permission");
            return true;
        }
        
        // Komut kontrolleri
        if (args.length == 0) {
            plugin.getMessageManager().sendMessage(player, "admin-help");
            return true;
        }
        
        String subCommand = args[0].toLowerCase();
        
        switch (subCommand) {
            case "reload":
                handleReload(player);
                break;
            case "arena":
                handleArenaCommand(player, Arrays.copyOfRange(args, 1, args.length));
                break;
            case "stats":
                handleStatsCommand(player, Arrays.copyOfRange(args, 1, args.length));
                break;
            default:
                plugin.getMessageManager().sendMessage(player, "admin-help");
                break;
        }
        
        return true;
    }
    
    /**
     * Yeniden yükleme komutunu işler
     * 
     * @param player Komutu kullanan oyuncu
     */
    private void handleReload(Player player) {
        // Config dosyasını yeniden yükle
        plugin.reloadConfig();
        
        // Mesaj yöneticisini yeniden yükle
        plugin.getMessageManager().reload();
        
        // Arena yöneticisini yeniden yükle
        arenaManager.reload();
        
        // Bildirim gönder
        plugin.getMessageManager().sendMessage(player, "config-reloaded");
    }
    
    /**
     * Arena komutlarını işler
     * 
     * @param player Komutu kullanan oyuncu
     * @param args Komut argümanları
     */
    private void handleArenaCommand(Player player, String[] args) {
        if (args.length == 0) {
            plugin.getMessageManager().sendMessage(player, "arena-help");
            return;
        }
        
        String arenaSubCommand = args[0].toLowerCase();
        
        switch (arenaSubCommand) {
            case "create":
                if (args.length < 2) {
                    player.sendMessage(ChatColor.RED + "Kullanım: /dueladmin arena create <arena-id> [arena-isim]");
                    return;
                }
                
                String arenaId = args[1];
                String arenaName = args.length > 2 ? String.join(" ", Arrays.copyOfRange(args, 2, args.length)) : arenaId;
                
                Location playerLoc = player.getLocation();
                Arena arena = new Arena(arenaId, arenaName, playerLoc.getWorld(), playerLoc, playerLoc);
                
                // Arenayı oluştur
                arenaManager.createArena(arena);
                
                player.sendMessage(ChatColor.GREEN + "Arena " + arenaId + " oluşturuldu. Şimdi pozisyonları ayarlayın.");
                break;
                
            case "setpos1":
                if (args.length < 2) {
                    player.sendMessage(ChatColor.RED + "Kullanım: /dueladmin arena setpos1 <arena-id>");
                    return;
                }
                
                arenaId = args[1];
                Arena targetArena = arenaManager.getArena(arenaId);
                
                if (targetArena == null) {
                    player.sendMessage(ChatColor.RED + "Arena bulunamadı: " + arenaId);
                    return;
                }
                
                // Pozisyon 1'i ayarla
                targetArena.setPos1(player.getLocation());
                arenaManager.saveArena(targetArena);
                
                player.sendMessage(ChatColor.GREEN + "Arena " + arenaId + " için 1. pozisyon ayarlandı.");
                break;
                
            case "setpos2":
                if (args.length < 2) {
                    player.sendMessage(ChatColor.RED + "Kullanım: /dueladmin arena setpos2 <arena-id>");
                    return;
                }
                
                arenaId = args[1];
                targetArena = arenaManager.getArena(arenaId);
                
                if (targetArena == null) {
                    player.sendMessage(ChatColor.RED + "Arena bulunamadı: " + arenaId);
                    return;
                }
                
                // Pozisyon 2'yi ayarla
                targetArena.setPos2(player.getLocation());
                arenaManager.saveArena(targetArena);
                
                player.sendMessage(ChatColor.GREEN + "Arena " + arenaId + " için 2. pozisyon ayarlandı.");
                break;
                
            case "enable":
                if (args.length < 2) {
                    player.sendMessage(ChatColor.RED + "Kullanım: /dueladmin arena enable <arena-id>");
                    return;
                }
                
                arenaId = args[1];
                targetArena = arenaManager.getArena(arenaId);
                
                if (targetArena == null) {
                    player.sendMessage(ChatColor.RED + "Arena bulunamadı: " + arenaId);
                    return;
                }
                
                // Arenayı etkinleştir
                targetArena.setEnabled(true);
                arenaManager.saveArena(targetArena);
                
                player.sendMessage(ChatColor.GREEN + "Arena " + arenaId + " etkinleştirildi.");
                break;
            case "disable":
                if (args.length < 2) {
                    player.sendMessage(ChatColor.RED + "Kullanım: /dueladmin arena disable <arena-id>");
                    return;
                }
                
                arenaId = args[1];
                targetArena = arenaManager.getArena(arenaId);
                
                if (targetArena == null) {
                    player.sendMessage(ChatColor.RED + "Arena bulunamadı: " + arenaId);
                    return;
                }
                
                // Arenayı devre dışı bırak
                targetArena.setEnabled(false);
                arenaManager.saveArena(targetArena);
                
                player.sendMessage(ChatColor.GREEN + "Arena " + arenaId + " devre dışı bırakıldı.");
                break;
                
            case "delete":
                if (args.length < 2) {
                    player.sendMessage(ChatColor.RED + "Kullanım: /dueladmin arena delete <arena-id>");
                    return;
                }
                
                arenaId = args[1];
                
                // Arenayı sil
                if (arenaManager.deleteArena(arenaId)) {
                    player.sendMessage(ChatColor.GREEN + "Arena " + arenaId + " silindi.");
                } else {
                    player.sendMessage(ChatColor.RED + "Arena bulunamadı: " + arenaId);
                }
                break;
                
            case "list":
                // Tüm arenaları listele
                Collection<Arena> arenas = arenaManager.getArenas().values();
                
                if (arenas.isEmpty()) {
                    player.sendMessage(ChatColor.YELLOW + "Hiç arena bulunamadı.");
                    return;
                }
                
                player.sendMessage(ChatColor.GREEN + "Arenalar:");
                
                for (Arena a : arenas) {
                    String status = a.isEnabled() ? ChatColor.GREEN + "Aktif" : ChatColor.RED + "Devre Dışı";
                    player.sendMessage(ChatColor.GOLD + "- " + a.getId() + " (" + a.getName() + ") " + status);
                }
                break;
                
            default:
                plugin.getMessageManager().sendMessage(player, "arena-help");
                break;
        }
    }
    
    /**
     * İstatistik komutlarını işler
     * 
     * @param player Komutu kullanan oyuncu
     * @param args Komut argümanları
     */
    private void handleStatsCommand(Player player, String[] args) {
        if (args.length == 0) {
            player.sendMessage(ChatColor.RED + "Kullanım: /dueladmin stats <reset|reload>");
            return;
        }
        
        String statsSubCommand = args[0].toLowerCase();
        
        switch (statsSubCommand) {
            case "reload":
                // İstatistikleri yeniden yükle
                plugin.getStatsManager().saveAllStats();
                
                player.sendMessage(ChatColor.GREEN + "İstatistikler veritabanına kaydedildi.");
                break;
                
            default:
                player.sendMessage(ChatColor.RED + "Kullanım: /dueladmin stats <reset|reload>");
                break;
        }
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!(sender instanceof Player)) {
            return new ArrayList<>();
        }
        
        Player player = (Player) sender;
        
        if (!player.hasPermission("cduello.admin")) {
            return new ArrayList<>();
        }
        
        List<String> completions = new ArrayList<>();
        
        if (args.length == 1) {
            List<String> subCommands = Arrays.asList("reload", "arena", "stats");
            return filterCompletions(subCommands, args[0]);
        }
        
        if (args.length == 2) {
            if (args[0].equalsIgnoreCase("arena")) {
                List<String> arenaSubCommands = Arrays.asList(
                    "create", "setpos1", "setpos2", "enable", "disable", "delete", "list"
                );
                return filterCompletions(arenaSubCommands, args[1]);
            } else if (args[0].equalsIgnoreCase("stats")) {
                List<String> statsSubCommands = Arrays.asList("reload");
                return filterCompletions(statsSubCommands, args[1]);
            }
        }
        
        if (args.length == 3) {
            if (args[0].equalsIgnoreCase("arena") && 
               (args[1].equalsIgnoreCase("setpos1") || 
                args[1].equalsIgnoreCase("setpos2") || 
                args[1].equalsIgnoreCase("enable") || 
                args[1].equalsIgnoreCase("disable") || 
                args[1].equalsIgnoreCase("delete"))) {
                // Arena ID'lerini tamamla
                List<String> arenaIds = new ArrayList<>(arenaManager.getArenas().keySet());
                return filterCompletions(arenaIds, args[2]);
            }
        }
        
        return completions;
    }
    
    /**
     * Tamamlama filtresi
     * 
     * @param options Tamamlama seçenekleri
     * @param arg Mevcut argüman
     * @return Filtrelenmiş tamamlamalar
     */
    private List<String> filterCompletions(List<String> options, String arg) {
        if (arg.isEmpty()) {
            return options;
        }
        
        String lowerArg = arg.toLowerCase();
        
        return options.stream()
                    .filter(option -> option.toLowerCase().startsWith(lowerArg))
                    .collect(Collectors.toList());
    }
} 