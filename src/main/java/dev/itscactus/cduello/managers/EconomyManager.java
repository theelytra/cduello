package dev.itscactus.cduello.managers;

import dev.itscactus.cduello.Main;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;

import java.text.NumberFormat;
import java.util.Locale;

public class EconomyManager {
    private final Main plugin;
    private Economy economy;
    private boolean economyEnabled = false;
    private final double winnerPercentage;
    private final double minBetAmount;
    private final double maxBetAmount;
    private final double announcementThreshold;

    public EconomyManager(Main plugin) {
        this.plugin = plugin;
        this.winnerPercentage = plugin.getConfig().getDouble("economy.winner-percentage", 80.0);
        this.minBetAmount = plugin.getConfig().getDouble("economy.min-bet-amount", 10.0);
        this.maxBetAmount = plugin.getConfig().getDouble("economy.max-bet-amount", 1000000.0);
        this.announcementThreshold = plugin.getConfig().getDouble("economy.announcement-threshold", 200000.0);
        setupEconomy();
    }

    private boolean setupEconomy() {
        if (Bukkit.getPluginManager().getPlugin("Vault") == null) {
            plugin.getLogger().warning("Vault bulunamadı. Ekonomi özellikleri devre dışı.");
            return false;
        }

        RegisteredServiceProvider<Economy> rsp = plugin.getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            plugin.getLogger().warning("Ekonomi servisi bulunamadı. Ekonomi özellikleri devre dışı.");
            return false;
        }

        economy = rsp.getProvider();
        economyEnabled = true;
        plugin.getLogger().info("Vault ekonomi entegrasyonu başarıyla kuruldu.");
        return true;
    }

    public boolean isEconomyEnabled() {
        return economyEnabled;
    }

    public Economy getEconomy() {
        return economy;
    }

    public boolean hasSufficientFunds(Player player, double amount) {
        if (!economyEnabled) return false;
        return economy.has(player, amount);
    }

    public boolean withdrawMoney(Player player, double amount) {
        if (!economyEnabled) return false;
        if (!hasSufficientFunds(player, amount)) return false;
        
        return economy.withdrawPlayer(player, amount).transactionSuccess();
    }

    public boolean depositMoney(OfflinePlayer player, double amount) {
        if (!economyEnabled) return false;
        return economy.depositPlayer(player, amount).transactionSuccess();
    }

    public double getWinnerPercentage() {
        return winnerPercentage;
    }

    public double getMinBetAmount() {
        return minBetAmount;
    }

    public double getMaxBetAmount() {
        return maxBetAmount;
    }

    public double getAnnouncementThreshold() {
        return announcementThreshold;
    }

    public boolean shouldAnnounce(double amount) {
        return amount >= announcementThreshold;
    }

    public String formatMoney(double amount) {
        NumberFormat format = NumberFormat.getNumberInstance(new Locale("tr", "TR"));
        format.setMaximumFractionDigits(2);
        return format.format(amount);
    }
} 