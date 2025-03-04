package dev.itscactus.cduello.utils;

import dev.itscactus.cduello.Main;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.util.Map;

public class MessageManager {
    private final Main plugin;
    private final FileConfiguration config;
    private final MiniMessage miniMessage;
    private final BukkitAudiences adventure;

    public MessageManager(Main plugin) {
        this.plugin = plugin;
        this.config = plugin.getConfig();
        this.miniMessage = MiniMessage.miniMessage();
        this.adventure = BukkitAudiences.create(plugin);
    }

    /**
     * Yapılandırma dosyasından mesaj alır ve MiniMessage ile formatlar
     *
     * @param path Mesajın yapılandırma dosyasındaki yolu
     * @return Formatlanmış mesaj
     */
    public Component getMessage(String path) {
        String message = config.getString("messages." + path, "Mesaj bulunamadı: " + path);
        return miniMessage.deserialize(convertLegacyToMiniMessage(message));
    }

    /**
     * Yapılandırma dosyasından mesaj alır, yer tutucuları değiştirir ve MiniMessage ile formatlar
     *
     * @param path Mesajın yapılandırma dosyasındaki yolu
     * @param placeholders Yer tutucu -> değer eşleşmeleri
     * @return Formatlanmış mesaj
     */
    public Component getMessage(String path, Map<String, String> placeholders) {
        String message = config.getString("messages." + path, "Mesaj bulunamadı: " + path);
        
        // Yer tutucuları değiştir
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            message = message.replace("%" + entry.getKey() + "%", entry.getValue());
        }
        
        return miniMessage.deserialize(convertLegacyToMiniMessage(message));
    }

    /**
     * Bir oyuncuya MiniMessage formatında mesaj gönderir
     *
     * @param player Mesajı alacak oyuncu
     * @param component Gönderilecek mesaj
     */
    public void sendMessage(Player player, Component component) {
        adventure.player(player).sendMessage(component);
    }

    /**
     * Bir komut göndericisine MiniMessage formatında mesaj gönderir
     *
     * @param sender Mesajı alacak komut göndericisi
     * @param component Gönderilecek mesaj
     */
    public void sendMessage(CommandSender sender, Component component) {
        adventure.sender(sender).sendMessage(component);
    }

    /**
     * Bir oyuncuya yapılandırma dosyasından mesaj alıp gönderir
     *
     * @param player Mesajı alacak oyuncu
     * @param path Mesajın yapılandırma dosyasındaki yolu
     */
    public void sendMessage(Player player, String path) {
        sendMessage(player, getMessage(path));
    }

    /**
     * Bir oyuncuya yapılandırma dosyasından mesaj alıp, yer tutucuları değiştirip gönderir
     *
     * @param player Mesajı alacak oyuncu
     * @param path Mesajın yapılandırma dosyasındaki yolu
     * @param placeholders Yer tutucu -> değer eşleşmeleri
     */
    public void sendMessage(Player player, String path, Map<String, String> placeholders) {
        sendMessage(player, getMessage(path, placeholders));
    }

    /**
     * Bir komut göndericisine yapılandırma dosyasından mesaj alıp gönderir
     *
     * @param sender Mesajı alacak komut göndericisi
     * @param path Mesajın yapılandırma dosyasındaki yolu
     */
    public void sendMessage(CommandSender sender, String path) {
        sendMessage(sender, getMessage(path));
    }

    /**
     * Bir komut göndericisine yapılandırma dosyasından mesaj alıp, yer tutucuları değiştirip gönderir
     *
     * @param sender Mesajı alacak komut göndericisi
     * @param path Mesajın yapılandırma dosyasındaki yolu
     * @param placeholders Yer tutucu -> değer eşleşmeleri
     */
    public void sendMessage(CommandSender sender, String path, Map<String, String> placeholders) {
        sendMessage(sender, getMessage(path, placeholders));
    }

    /**
     * Eski renk kodlarını (&) MiniMessage formatına dönüştürür
     *
     * @param input Dönüştürülecek metin
     * @return MiniMessage formatında metin
     */
    public String convertLegacyToMiniMessage(String input) {
        if (input == null) return "";
        
        // Eski renk kodlarını MiniMessage formatına dönüştür
        return input
                .replace("&0", "<black>")
                .replace("&1", "<dark_blue>")
                .replace("&2", "<dark_green>")
                .replace("&3", "<dark_aqua>")
                .replace("&4", "<dark_red>")
                .replace("&5", "<dark_purple>")
                .replace("&6", "<gold>")
                .replace("&7", "<gray>")
                .replace("&8", "<dark_gray>")
                .replace("&9", "<blue>")
                .replace("&a", "<green>")
                .replace("&b", "<aqua>")
                .replace("&c", "<red>")
                .replace("&d", "<light_purple>")
                .replace("&e", "<yellow>")
                .replace("&f", "<white>")
                .replace("&l", "<bold>")
                .replace("&m", "<strikethrough>")
                .replace("&n", "<underline>")
                .replace("&o", "<italic>")
                .replace("&r", "<reset>");
    }

    /**
     * MiniMessage'ı doğrudan dizelere uygular
     * 
     * @param input Formatlanacak metin
     * @return Formatlanmış Component
     */
    public Component format(String input) {
        return miniMessage.deserialize(convertLegacyToMiniMessage(input));
    }

    /**
     * Ön eki getirir
     * 
     * @return Formatlı öneki
     */
    public Component getPrefix() {
        return getMessage("prefix");
    }

    /**
     * Tüm kaynakları serbest bırakır
     */
    public void close() {
        if (adventure != null) {
            adventure.close();
        }
    }
} 