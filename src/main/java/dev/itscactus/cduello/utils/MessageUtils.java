package dev.itscactus.cduello.utils;

import dev.itscactus.cduello.Main;
import net.kyori.adventure.text.Component;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Map;

/**
 * MessageManager sınıfına geçişi kolaylaştırmak için
 * Backward compatibility sınıfı (eski kod uyumluluğu için)
 */
public class MessageUtils {

    private static Main plugin;
    
    /**
     * Eski şekilde renk kodlarını dönüştürür, yeni kodlar için MessageManager kullanın
     * 
     * @param input Renk kodlarıyla metin
     * @return Renklendirilmiş metin
     */
    public static String color(String input) {
        if (input == null) return "";
        return ChatColor.translateAlternateColorCodes('&', input);
    }
    
    /**
     * Mesaj gönderir
     * 
     * @param sender Alıcı
     * @param message Mesaj
     */
    public static void sendMessage(CommandSender sender, String message) {
        if (plugin != null) {
            plugin.getMessageManager().sendMessage(sender, plugin.getMessageManager().format(message));
        } else {
            sender.sendMessage(color(message));
        }
    }
    
    /**
     * Config'den mesaj alır
     * 
     * @param path Mesaj yolu
     * @return Formatlı mesaj
     */
    public static String getMessage(String path) {
        if (plugin != null) {
            Component component = plugin.getMessageManager().getMessage(path);
            // Adventure API kullananlar için component'i kullanın
            // Bu sadece String geri dönüşü için
            return path;
        }
        return "Mesaj bulunamadı: " + path;
    }
    
    /**
     * Config'den mesaj alır ve yer tutucuları değiştirir
     * 
     * @param path Mesaj yolu
     * @param placeholders Yer tutucular
     * @return Formatlı mesaj
     */
    public static String getMessage(String path, Map<String, String> placeholders) {
        if (plugin != null) {
            Component component = plugin.getMessageManager().getMessage(path, placeholders);
            // Adventure API kullananlar için component'i kullanın
            // Bu sadece String geri dönüşü için
            return path;
        }
        return "Mesaj bulunamadı: " + path;
    }
    
    /**
     * Öneki döndürür
     * 
     * @return Formatlı prefix
     */
    public static String getPrefix() {
        if (plugin != null) {
            return plugin.getConfig().getString("messages.prefix", "&8[&bcDuello&8] &r");
        }
        return "&8[&bcDuello&8] &r";
    }
    
    /**
     * Sınıfı başlatır
     * 
     * @param main Ana sınıf
     */
    public static void initialize(Main main) {
        plugin = main;
    }
} 