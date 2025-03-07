package dev.itscactus.cduello.managers;

import dev.itscactus.cduello.Main;
import dev.itscactus.cduello.models.Duel;
import dev.itscactus.cduello.models.DuelRequest;
import dev.itscactus.cduello.models.PlayerStats;
import dev.itscactus.cduello.models.Arena;
import dev.itscactus.cduello.utils.MessageManager;
import dev.itscactus.cduello.utils.MessageUtils;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.Location;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.concurrent.ThreadLocalRandom;

import dev.itscactus.cduello.listeners.DuelListener;

public class DuelManager {

    private final Main plugin;
    private final EconomyManager economyManager;
    private final MessageManager messageManager;
    private final Map<UUID, DuelRequest> pendingRequests = new ConcurrentHashMap<>();
    private final Map<UUID, Duel> activeDuels = new ConcurrentHashMap<>();
    private final Map<UUID, PlayerStats> playerStats = new ConcurrentHashMap<>();
    private final Set<UUID> playersInDuel = new HashSet<>();
    private final Set<UUID> playersInCountdown = new HashSet<>();
    private ArenaManager arenaManager;
    private DuelListener duelListener;

    public DuelManager(Main plugin, EconomyManager economyManager) {
        this.plugin = plugin;
        this.economyManager = economyManager;
        this.messageManager = plugin.getMessageManager();
    }

    /**
     * ArenaManager'ı ayarlar
     * 
     * @param arenaManager Arena yöneticisi
     */
    public void setArenaManager(ArenaManager arenaManager) {
        this.arenaManager = arenaManager;
    }

    /**
     * DuelListener'ı ayarlar (optimize edilmiş countdown takibi için)
     *
     * @param duelListener Listener instance
     */
    public void setDuelListener(DuelListener duelListener) {
        this.duelListener = duelListener;
    }

    /**
     * Normal bir düello isteği gönderir
     *
     * @param sender İsteği gönderen oyuncu
     * @param target İstek alıcısı oyuncu
     */
    public void sendDuelRequest(Player sender, Player target) {
        sendDuelRequest(sender, target, 0);
    }

    /**
     * Para ödüllü bir düello isteği gönderir
     *
     * @param sender İsteği gönderen oyuncu
     * @param target İstek alıcısı oyuncu
     * @param betAmount Bahis miktarı
     */
    public void sendDuelRequest(Player sender, Player target, double betAmount) {
        // Oyuncunun zaten bir düelloda olup olmadığını kontrol et
        if (isInDuel(sender.getUniqueId())) {
            Map<String, String> placeholders = new HashMap<>();
            messageManager.sendMessage(sender, "already-in-duel");
            return;
        }

        // Hedef oyuncunun zaten bir düelloda olup olmadığını kontrol et
        if (isInDuel(target.getUniqueId())) {
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("player", target.getName());
            messageManager.sendMessage(sender, "target-in-duel", placeholders);
            return;
        }

        // Para düellosu kontrolleri
        if (betAmount > 0) {
            if (!economyManager.isEconomyEnabled()) {
                messageManager.sendMessage(sender, "economy-not-enabled");
                return;
            }

            // Gönderenin yeterli parası var mı kontrol et
            if (!economyManager.hasSufficientFunds(sender, betAmount)) {
                Map<String, String> placeholders = new HashMap<>();
                placeholders.put("amount", economyManager.formatMoney(betAmount));
                messageManager.sendMessage(sender, "insufficient-funds", placeholders);
                return;
            }

            // Hedefin yeterli parası var mı kontrol et
            if (!economyManager.hasSufficientFunds(target, betAmount)) {
                Map<String, String> placeholders = new HashMap<>();
                placeholders.put("player", target.getName());
                messageManager.sendMessage(sender, "target-insufficient-funds", placeholders);
                return;
            }
        }

        // İsteği oluştur
        UUID senderUuid = sender.getUniqueId();
        UUID targetUuid = target.getUniqueId();

        DuelRequest request = new DuelRequest(senderUuid, targetUuid);
        
        if (betAmount > 0) {
            request.setBetAmount(betAmount);
        }

        // Varsa eski isteği kaldır
        for (Map.Entry<UUID, DuelRequest> entry : pendingRequests.entrySet()) {
            DuelRequest existingRequest = entry.getValue();
            if (existingRequest.getSender().equals(senderUuid) && existingRequest.getTarget().equals(targetUuid)) {
                pendingRequests.remove(entry.getKey());
                break;
            }
        }

        // Yeni isteği ekle
        pendingRequests.put(targetUuid, request);

        // Mesajları gönder
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("player", target.getName());
        
        if (betAmount > 0) {
            placeholders.put("amount", economyManager.formatMoney(betAmount));
            messageManager.sendMessage(sender, "duel-request-sent-money", placeholders);
            
            placeholders.clear();
            placeholders.put("player", sender.getName());
            placeholders.put("amount", economyManager.formatMoney(betAmount));
            messageManager.sendMessage(target, "duel-request-received-money", placeholders);
        } else {
            messageManager.sendMessage(sender, "duel-request-sent", placeholders);
            
            placeholders.clear();
            placeholders.put("player", sender.getName());
            messageManager.sendMessage(target, "duel-request-received", placeholders);
        }

        // Zaman aşımı için görev zamanlama
        int timeout = plugin.getConfig().getInt("duels.request-timeout", 30);
        
        new BukkitRunnable() {
            @Override
            public void run() {
                // İstek hala geçerli mi kontrol et
                if (pendingRequests.containsKey(targetUuid)) {
                    DuelRequest expiredRequest = pendingRequests.get(targetUuid);
                    if (expiredRequest.equals(request)) {
                        pendingRequests.remove(targetUuid);

                        // Hedef ve gönderici hala çevrimiçi mi kontrol et
                        Player expiredSender = Bukkit.getPlayer(senderUuid);
                        Player expiredTarget = Bukkit.getPlayer(targetUuid);

                        // Mesajları gönder
                        if (expiredSender != null) {
                            Map<String, String> timeoutPlaceholders = new HashMap<>();
                            timeoutPlaceholders.put("player", target.getName());
                            messageManager.sendMessage(expiredSender, "duel-request-timeout-sender", timeoutPlaceholders);
                        }

                        if (expiredTarget != null) {
                            Map<String, String> timeoutPlaceholders = new HashMap<>();
                            timeoutPlaceholders.put("player", sender.getName());
                            messageManager.sendMessage(expiredTarget, "duel-request-timeout-target", timeoutPlaceholders);
                        }
                    }
                }
            }
        }.runTaskLater(plugin, timeout * 20L);
    }

    /**
     * Bir düello isteğini kabul eder
     *
     * @param player İsteği kabul eden oyuncu
     */
    public void acceptDuelRequest(Player player) {
        UUID playerUuid = player.getUniqueId();

        // İsteğin varlığını kontrol et
        if (!pendingRequests.containsKey(playerUuid)) {
            messageManager.sendMessage(player, "no-pending-requests");
            return;
        }

        DuelRequest request = pendingRequests.get(playerUuid);
        UUID senderUuid = request.getSender();
        Player sender = Bukkit.getPlayer(senderUuid);

        // Göndericinin çevrimiçi olup olmadığını kontrol et
        if (sender == null) {
            pendingRequests.remove(playerUuid);
            messageManager.sendMessage(player, "sender-offline");
            return;
        }

        // Oyuncunun zaten düelloda olup olmadığını kontrol et
        if (isInDuel(playerUuid)) {
            pendingRequests.remove(playerUuid);
            messageManager.sendMessage(player, "already-in-duel");
            return;
        }

        // Göndericinin zaten düelloda olup olmadığını kontrol et
        if (isInDuel(senderUuid)) {
            pendingRequests.remove(playerUuid);
            messageManager.sendMessage(player, "duel-status-changed");
            return;
        }

        // Para düellosu için ek kontroller
        double betAmount = request.getBetAmount();
        if (request.isMoneyDuel()) {
            // Gönderenin hala yeterli parası var mı kontrol et
            if (!economyManager.hasSufficientFunds(sender, betAmount)) {
                pendingRequests.remove(playerUuid);
                Map<String, String> placeholders = new HashMap<>();
                placeholders.put("player", sender.getName());
                messageManager.sendMessage(player, "sender-insufficient-funds", placeholders);
                return;
            }

            // Kabul edenin yeterli parası var mı kontrol et
            if (!economyManager.hasSufficientFunds(player, betAmount)) {
                pendingRequests.remove(playerUuid);
                Map<String, String> placeholders = new HashMap<>();
                placeholders.put("amount", economyManager.formatMoney(betAmount));
                messageManager.sendMessage(player, "insufficient-funds", placeholders);
                return;
            }
        }

        // İsteği kaldır
        pendingRequests.remove(playerUuid);

        // Kabul mesajını gönder
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("player", player.getName());
        messageManager.sendMessage(sender, "duel-accepted", placeholders);

        // Düelloyu başlat
        if (request.isMoneyDuel()) {
            startDuel(sender, player, betAmount);
        } else {
            startDuel(sender, player);
        }
    }

    /**
     * Bir düello isteğini reddeder
     *
     * @param player İsteği reddeden oyuncu
     */
    public void denyDuelRequest(Player player) {
        UUID playerUuid = player.getUniqueId();

        // İsteğin varlığını kontrol et
        if (!pendingRequests.containsKey(playerUuid)) {
            messageManager.sendMessage(player, "no-pending-requests");
            return;
        }

        DuelRequest request = pendingRequests.get(playerUuid);
        pendingRequests.remove(playerUuid);

        // Göndericiye mesaj gönder
        Player sender = Bukkit.getPlayer(request.getSender());
        if (sender != null) {
            Map<String, String> placeholders = new HashMap<>();
            placeholders.put("player", player.getName());
            messageManager.sendMessage(sender, "duel-request-denied-sender", placeholders);
        }

        // Red mesajını göster
        messageManager.sendMessage(player, "duel-request-denied-target");
    }

    /**
     * İki oyuncu arasında düello başlatır (para ödülü olmadan)
     */
    private void startDuel(Player player1, Player player2) {
        startDuel(player1, player2, 0.0);
    }

    /**
     * İki oyuncu arasında düello başlatır (belirlenen para ödülü ile)
     * Bu metot performans için optimize edilmiştir
     */
    private void startDuel(Player player1, Player player2, double betAmount) {
        // Oyuncuların lokasyonlarını sakla
        Location player1Loc = player1.getLocation().clone();
        Location player2Loc = player2.getLocation().clone();
        
        // Oyuncuları düelloda işaretle
        UUID player1Uuid = player1.getUniqueId();
        UUID player2Uuid = player2.getUniqueId();
        
        playersInDuel.add(player1Uuid);
        playersInDuel.add(player2Uuid);
        
        // Düello oluştur
        Duel duel = new Duel(player1, player2, player1Loc, player2Loc, betAmount);
        
        // Düelloyu aktif düellolar listesine ekle
        activeDuels.put(duel.getId(), duel);
        
        // Arena kullanımını kontrol et
        boolean useArenas = plugin.getConfig().getBoolean("duels.arenas.enabled", true) && 
                           arenaManager != null && 
                           arenaManager.isEnabled() && 
                           !arenaManager.getArenas().isEmpty();
        
        // Debug log
        if (plugin.getConfig().getBoolean("debug", false)) {
            plugin.getLogger().info("Arena usage check: " + useArenas + 
                                    " (config: " + plugin.getConfig().getBoolean("duels.arenas.enabled", true) + 
                                    ", manager: " + (arenaManager != null) + 
                                    ", enabled: " + (arenaManager != null && arenaManager.isEnabled()) + 
                                    ", arenas: " + (arenaManager != null && !arenaManager.getArenas().isEmpty()) + ")");
        }
        
        Map<String, String> placeholders = null;
        Arena selectedArena = null;
        
        if (useArenas) {
            // Uygun arenaları filtrele (etkin olanlar)
            List<Arena> availableArenas = arenaManager.getArenas().values().stream()
                    .filter(Arena::isEnabled)
                    .filter(arena -> arena.getPos1() != null && arena.getPos2() != null)
                    .collect(Collectors.toList());
            
            // Debug log
            if (plugin.getConfig().getBoolean("debug", false)) {
                plugin.getLogger().info("Available arenas: " + availableArenas.size());
                for (Arena arena : availableArenas) {
                    plugin.getLogger().info(" - " + arena.getName() + " (enabled: " + arena.isEnabled() + 
                                           ", pos1: " + (arena.getPos1() != null) + 
                                           ", pos2: " + (arena.getPos2() != null) + ")");
                }
            }
            
            if (!availableArenas.isEmpty()) {
                // Rastgele arena seç (çok sayıda arena olduğunda performans için optimizasyon)
                int randomIndex = ThreadLocalRandom.current().nextInt(availableArenas.size());
                selectedArena = availableArenas.get(randomIndex);
                
                // Debug log
                if (plugin.getConfig().getBoolean("debug", false)) {
                    plugin.getLogger().info("Selected arena: " + selectedArena.getName());
                }
                
                // Arena konumlarının null olmadığından emin ol
                if (selectedArena.getPos1() != null && selectedArena.getPos2() != null) {
                    // Oyuncuların hala çevrimiçi olduğundan emin ol
                    if (player1.isOnline() && player2.isOnline()) {
                        try {
                            // Clone and add 0.5 offset to center players on blocks and ensure they're on solid ground
                            Location pos1 = selectedArena.getPos1().clone().add(0.5, 0.1, 0.5);
                            Location pos2 = selectedArena.getPos2().clone().add(0.5, 0.1, 0.5);
                            
                            // Debug log
                            if (plugin.getConfig().getBoolean("debug", false)) {
                                plugin.getLogger().info("Teleporting players to arena:");
                                plugin.getLogger().info(" - Player1: " + player1.getName() + " to " + 
                                                      pos1.getWorld().getName() + "," + 
                                                      pos1.getX() + "," + pos1.getY() + "," + pos1.getZ());
                                plugin.getLogger().info(" - Player2: " + player2.getName() + " to " + 
                                                      pos2.getWorld().getName() + "," + 
                                                      pos2.getX() + "," + pos2.getY() + "," + pos2.getZ());
                            }
                            
                            // First save original locations to return to later
                            duel.setChallengerLocation(player1Loc);
                            duel.setChallengedLocation(player2Loc);
                            
                            // Then teleport players with a slight delay to ensure the teleport completes
                            new BukkitRunnable() {
                                @Override
                                public void run() {
                                    if (player1.isOnline()) {
                                        player1.teleport(pos1);
                                    }
                                }
                            }.runTask(plugin);
                            
                            new BukkitRunnable() {
                                @Override
                                public void run() {
                                    if (player2.isOnline()) {
                                        player2.teleport(pos2);
                                    }
                                }
                            }.runTask(plugin);
                            
                            // Arena bilgisini placeholders'a ekle
                            placeholders = new HashMap<>();
                            placeholders.put("arena", selectedArena.getName());
                            
                            // Teleport mesajı gönder
                            messageManager.sendMessage(player1, "teleported-to-arena", placeholders);
                            messageManager.sendMessage(player2, "teleported-to-arena", placeholders);
                        } catch (Exception e) {
                            // Log any teleport errors
                            plugin.getLogger().severe("Error teleporting players to arena: " + e.getMessage());
                            e.printStackTrace();
                        }
                    }
                }
            }
        }
        
        // Oyuncuları geri sayıma ekle
        addPlayersToCountdown(player1, player2);
        
        // Geri sayımı başlat
        int countdown = plugin.getConfig().getInt("duels.countdown", 5);
        
        // Geri sayım görevi
        new BukkitRunnable() {
            int secondsLeft = countdown;
            
            @Override
            public void run() {
                // Oyunculardan biri çevrimiçi değilse, düelloyu iptal et
                if (!player1.isOnline() || !player2.isOnline()) {
                    endDuelPrematurely(duel);
                    cancel();
                    return;
                }
                
                // Geri sayım tamamlandı, düelloyu başlat
                if (secondsLeft <= 0) {
                    // Düello durumunu güncelle
                    duel.setState(Duel.DuelState.ACTIVE);
                    
                    // Oyuncuları hareketsiz moddan çıkar
                    removePlayersFromCountdown(player1, player2);
                    
                    // Düello başlangıç mesajını gönder
                    if (betAmount > 0) {
                        // Para ödüllü düello
                        Map<String, String> msgPlaceholders = new HashMap<>();
                        msgPlaceholders.put("amount", economyManager.formatMoney(betAmount));
                        
                        messageManager.sendMessage(player1, "duel-started-money", msgPlaceholders);
                        messageManager.sendMessage(player2, "duel-started-money", msgPlaceholders);
                        
                        // Yüksek değerli düello ise duyuru yap
                        double announcementThreshold = plugin.getConfig().getDouble("economy.announcement-threshold", 200000.0);
                        
                        if (betAmount * 2 >= announcementThreshold) {
                            Map<String, String> announcePlaceholders = new HashMap<>();
                            announcePlaceholders.put("player1", player1.getName());
                            announcePlaceholders.put("player2", player2.getName());
                            announcePlaceholders.put("amount", economyManager.formatMoney(betAmount * 2));
                            
                            // Sunucudaki herkese duyur
                            for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                                messageManager.sendMessage(onlinePlayer, "high-value-duel-announcement", announcePlaceholders);
                            }
                        }
                    } else {
                        // Normal düello
                        messageManager.sendMessage(player1, "duel-started");
                        messageManager.sendMessage(player2, "duel-started");
                    }
                    
                    cancel();
                } else {
                    // Geri sayım mesajını gönder
                    Map<String, String> countPlaceholders = new HashMap<>();
                    countPlaceholders.put("seconds", String.valueOf(secondsLeft));
                    
                    if (betAmount > 0) {
                        // Para ödüllü düello
                        countPlaceholders.put("amount", economyManager.formatMoney(betAmount));
                        
                        messageManager.sendMessage(player1, "duel-countdown-money", countPlaceholders);
                        messageManager.sendMessage(player2, "duel-countdown-money", countPlaceholders);
                    } else {
                        // Normal düello
                        messageManager.sendMessage(player1, "duel-countdown", countPlaceholders);
                        messageManager.sendMessage(player2, "duel-countdown", countPlaceholders);
                    }
                    
                    secondsLeft--;
                }
            }
        }.runTaskTimer(plugin, 0L, 20L);
    }

    /**
     * Bir düelloyu bitirir
     *
     * @param duel Düello
     * @param winnerUuid Kazananın UUID'si
     */
    public void endDuel(Duel duel, UUID winnerUuid) {
        if (duel.getState() != Duel.DuelState.ACTIVE) {
            return;
        }

        // Düello durumunu güncelle
        duel.setState(Duel.DuelState.FINISHED);
        duel.setWinner(winnerUuid);

        UUID loserUuid = duel.getOpponent(winnerUuid);

        // Oyuncuları listeden çıkar
        playersInDuel.remove(winnerUuid);
        playersInDuel.remove(loserUuid);

        // İstatistikleri güncelle
        PlayerStats winnerStats = getPlayerStats(winnerUuid);
        PlayerStats loserStats = getPlayerStats(loserUuid);

        winnerStats.addWin();
        loserStats.addLoss();

        // Kazanan ve kaybedene mesaj gönder
        Player winner = Bukkit.getPlayer(winnerUuid);
        Player loser = Bukkit.getPlayer(loserUuid);

        // Para düellosu işlemleri
        if (duel.isMoneyDuel()) {
            double totalPot = duel.getTotalPot();
            double winnerAmount = duel.getWinnerAmount(economyManager.getWinnerPercentage());
            
            // Para istatistiklerini güncelle
            winnerStats.addMoneyWon(winnerAmount);
            loserStats.addMoneyLost(duel.getBetAmount());
            
            // Parayı öde
            economyManager.depositMoney(Bukkit.getOfflinePlayer(winnerUuid), winnerAmount);

            // Mesajları gönder
            if (winner != null) {
                Map<String, String> winPlaceholders = new HashMap<>();
                winPlaceholders.put("amount", economyManager.formatMoney(winnerAmount));
                messageManager.sendMessage(winner, "duel-won-money", winPlaceholders);
            }

            if (loser != null) {
                Map<String, String> losePlaceholders = new HashMap<>();
                losePlaceholders.put("amount", economyManager.formatMoney(duel.getBetAmount()));
                messageManager.sendMessage(loser, "duel-lost-money", losePlaceholders);
            }

            // Yüksek değerli düello sonuç duyurusu
            if (economyManager.shouldAnnounce(totalPot)) {
                Map<String, String> announcePlaceholders = new HashMap<>();
                announcePlaceholders.put("winner", winner != null ? winner.getName() : "Bilinmeyen");
                announcePlaceholders.put("loser", loser != null ? loser.getName() : "Bilinmeyen");
                announcePlaceholders.put("amount", economyManager.formatMoney(totalPot));
                announcePlaceholders.put("winnings", economyManager.formatMoney(winnerAmount));
                
                for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                    messageManager.sendMessage(onlinePlayer, "high-value-duel-result", announcePlaceholders);
                }
            }
        } else {
            // Normal düello mesajları
            if (winner != null) {
                messageManager.sendMessage(winner, "duel-won");
            }

            if (loser != null) {
                messageManager.sendMessage(loser, "duel-lost");
            }
        }

        // Orijinal lokasyonlara geri ışınlama
        boolean teleportBack = plugin.getConfig().getBoolean("duels.teleport-back", true);
        
        if (teleportBack) {
            if (winner != null) {
                winner.teleport(duel.getChallengerLocation());
            }
            
            if (loser != null) {
                loser.teleport(duel.getChallengedLocation());
            }
        }

        // Oyuncuları iyileştirme
        boolean healPlayers = plugin.getConfig().getBoolean("duels.heal-after-duel", true);
        
        if (healPlayers) {
            if (winner != null) {
                winner.setHealth(winner.getMaxHealth());
                winner.setFoodLevel(20);
            }
            
            if (loser != null) {
                loser.setHealth(loser.getMaxHealth());
                loser.setFoodLevel(20);
            }
        }

        // Efektleri temizleme
        boolean clearEffects = plugin.getConfig().getBoolean("duels.clear-effects", true);
        
        if (clearEffects) {
            if (winner != null) {
                winner.getActivePotionEffects().forEach(effect -> winner.removePotionEffect(effect.getType()));
            }
            
            if (loser != null) {
                loser.getActivePotionEffects().forEach(effect -> loser.removePotionEffect(effect.getType()));
            }
        }

        // Aktif düellolar listesinden kaldır
        activeDuels.remove(duel.getId());

        // Düellodan sonra playersInCountdown'u güncelle
        if (duelListener != null) {
            duelListener.updateCountdownPlayers(playersInCountdown);
        }
    }

    /**
     * Bir düelloyu erken bitirir (oyuncu çıktı vb.)
     *
     * @param duel Düello
     */
    private void endDuelPrematurely(Duel duel) {
        // Düello durumunu güncelle
        duel.setState(Duel.DuelState.CANCELLED);

        // Oyuncuları listeden çıkar
        playersInDuel.remove(duel.getChallenger());
        playersInDuel.remove(duel.getChallenged());

        // Para iadesi yap (para düellosu ise)
        if (duel.isMoneyDuel()) {
            double betAmount = duel.getBetAmount();
            economyManager.depositMoney(Bukkit.getOfflinePlayer(duel.getChallenger()), betAmount);
            economyManager.depositMoney(Bukkit.getOfflinePlayer(duel.getChallenged()), betAmount);
        }

        // Aktif düellolar listesinden kaldır
        activeDuels.remove(duel.getId());
    }

    /**
     * Bir oyuncunun sayım sürecinde olup olmadığını kontrol eder
     *
     * @param playerUuid Kontrol edilecek oyuncunun UUID'si
     * @return Oyuncu sayım sürecindeyse true, değilse false
     */
    public boolean isInCountdown(UUID playerUuid) {
        return playersInCountdown.contains(playerUuid);
    }

    /**
     * Oyuncuları geri sayım durumuna ekler
     *
     * @param player1 Birinci oyuncu
     * @param player2 İkinci oyuncu
     */
    private void addPlayersToCountdown(Player player1, Player player2) {
        // Eski metodu koruyalım
        playersInCountdown.add(player1.getUniqueId());
        playersInCountdown.add(player2.getUniqueId());
        
        // Optimize edilmiş versiyonu da güncelleyelim
        if (duelListener != null) {
            duelListener.updateCountdownPlayers(playersInCountdown);
        }
    }

    /**
     * Oyuncuları geri sayım durumundan çıkarır
     *
     * @param player1 Birinci oyuncu
     * @param player2 İkinci oyuncu
     */
    private void removePlayersFromCountdown(Player player1, Player player2) {
        // Eski metodu koruyalım
        playersInCountdown.remove(player1.getUniqueId());
        playersInCountdown.remove(player2.getUniqueId());
        
        // Optimize edilmiş versiyonu da güncelleyelim
        if (duelListener != null) {
            duelListener.updateCountdownPlayers(playersInCountdown);
        }
    }

    /**
     * Bir oyuncunun bekleyen düello isteği olup olmadığını kontrol eder
     *
     * @param player Oyuncu
     * @return Bekleyen istek varsa true, yoksa false
     */
    public boolean hasPendingRequests(Player player) {
        return pendingRequests.containsKey(player.getUniqueId());
    }

    /**
     * Bir oyuncunun istatistiklerini getirir
     *
     * @param uuid Oyuncu UUID
     * @return Oyuncu istatistikleri
     */
    private PlayerStats getPlayerStats(UUID uuid) {
        return playerStats.computeIfAbsent(uuid, k -> new PlayerStats(uuid));
    }

    /**
     * Bir oyuncunun istatistiklerini gösterir
     *
     * @param viewer İstatistikleri görüntüleyen oyuncu
     * @param target İstatistikleri görüntülenen oyuncu
     */
    public void showStats(Player viewer, Player target) {
        UUID targetUuid = target.getUniqueId();
        PlayerStats stats = getPlayerStats(targetUuid);

        int wins = stats.getWins();
        int losses = stats.getLosses();
        int totalDuels = wins + losses;
        double winRate = totalDuels > 0 ? (double) wins / totalDuels * 100 : 0;
        double moneyWon = stats.getMoneyWon();
        double moneyLost = stats.getMoneyLost();
        double netEarnings = stats.getNetEarnings();

        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("player", target.getName());
        placeholders.put("wins", String.valueOf(wins));
        placeholders.put("losses", String.valueOf(losses));
        placeholders.put("total", String.valueOf(totalDuels));
        placeholders.put("winrate", String.format("%.1f", winRate));
        
        if (economyManager.isEconomyEnabled()) {
            placeholders.put("money_won", economyManager.formatMoney(moneyWon));
            placeholders.put("money_lost", economyManager.formatMoney(moneyLost));
            placeholders.put("net_earnings", economyManager.formatMoney(netEarnings));
        }

        messageManager.sendMessage(viewer, "stats-header", placeholders);
        messageManager.sendMessage(viewer, "stats-wins", placeholders);
        messageManager.sendMessage(viewer, "stats-losses", placeholders);
        messageManager.sendMessage(viewer, "stats-total", placeholders);
        messageManager.sendMessage(viewer, "stats-winrate", placeholders);
        
        if (economyManager.isEconomyEnabled()) {
            messageManager.sendMessage(viewer, "stats-money-won", placeholders);
            messageManager.sendMessage(viewer, "stats-money-lost", placeholders);
            messageManager.sendMessage(viewer, "stats-net-earnings", placeholders);
        }
        
        messageManager.sendMessage(viewer, "stats-footer", placeholders);
    }

    /**
     * Bir oyuncunun aktif düellosunu getirir
     *
     * @param playerUuid Oyuncu UUID
     * @return Aktif düello
     */
    public Duel getActiveDuel(UUID playerUuid) {
        for (Duel duel : activeDuels.values()) {
            if (duel.hasPlayer(playerUuid)) {
                return duel;
            }
        }
        return null;
    }

    /**
     * Bir oyuncunun düelloda olup olmadığını kontrol eder
     *
     * @param playerUuid Oyuncu UUID
     * @return Düelloda ise true, değilse false
     */
    public boolean isInDuel(UUID playerUuid) {
        return playersInDuel.contains(playerUuid);
    }

    /**
     * Tüm aktif düelloları sonlandırır
     * 
     * @param reason Sonlandırma nedeni
     */
    public void endAllDuels(String reason) {
        // Kopyayla çalış ki ConcurrentModificationException oluşmasın
        List<Duel> duels = new ArrayList<>(activeDuels.values());
        
        for (Duel duel : duels) {
            // Her düelloyu iptal et
            Player challenger = Bukkit.getPlayer(duel.getChallenger());
            Player challenged = Bukkit.getPlayer(duel.getChallenged());
            
            // Oyunculara mesaj gönder
            if (challenger != null) {
                Map<String, String> placeholders = new HashMap<>();
                placeholders.put("reason", reason);
                messageManager.sendMessage(challenger, "duel-cancelled-admin", placeholders);
            }
            
            if (challenged != null) {
                Map<String, String> placeholders = new HashMap<>();
                placeholders.put("reason", reason);
                messageManager.sendMessage(challenged, "duel-cancelled-admin", placeholders);
            }
            
            // Düelloyu sonlandır
            endDuelPrematurely(duel);
        }
        
        // Tüm bekleyen istekleri temizle
        pendingRequests.clear();
    }
} 