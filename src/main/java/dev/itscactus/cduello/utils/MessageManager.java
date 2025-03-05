package dev.itscactus.cduello.utils;

import dev.itscactus.cduello.Main;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.scheduler.BukkitRunnable;

public class MessageManager {
    private final Main plugin;
    private final FileConfiguration config;
    private final MiniMessage miniMessage;
    private final BukkitAudiences adventure;

    // Mesaj önbelleği (sık kullanılan mesajlar için)
    private final Map<String, Component> messageCache = new ConcurrentHashMap<>();
    
    // Prefix önbelleği
    private Component cachedPrefix = null;
    
    // Önbellek geçerlilik süresi (milisaniye)
    private static final long CACHE_EXPIRY_MS = 60000; // 1 dakika
    
    // Son önbellek temizleme zamanı
    private long lastCacheClear = System.currentTimeMillis();

    public MessageManager(Main plugin) {
        this.plugin = plugin;
        this.config = plugin.getConfig();
        this.miniMessage = MiniMessage.miniMessage();
        this.adventure = BukkitAudiences.create(plugin);
    }

    /**
     * Verilen mesajı config dosyasından alır ve formatlar.
     * Performans için önbellek kullanır.
     *
     * @param key Mesaj keyi
     * @return Formatlanmış mesaj
     */
    public Component getMessage(String key) {
        // Cache'ten kontrol et
        String cacheKey = "msg:" + key;
        Component cachedMessage = messageCache.get(cacheKey);
        
        if (cachedMessage != null) {
            return cachedMessage;
        }
        
        // Cache'te yoksa, yeni mesajı oluştur
        String rawMessage = config.getString("messages." + key);
        
        if (rawMessage == null || rawMessage.isEmpty()) {
            return Component.empty();
        }
        
        Component formattedMessage = miniMessage.deserialize(rawMessage);
        
        // Cache'e kaydet
        messageCache.put(cacheKey, formattedMessage);
        checkCacheExpiry();
        
        return formattedMessage;
    }

    /**
     * Verilen mesajı config dosyasından alır, yer tutucuları değiştirir ve formatlar.
     * Performans için önbellek kullanır.
     *
     * @param key Mesaj keyi
     * @param placeholders Yer tutucular ve değerleri
     * @return Formatlanmış mesaj
     */
    public Component getMessage(String key, Map<String, String> placeholders) {
        // Mesaj yoksa boş dön
        String rawMessage = config.getString("messages." + key);
        
        if (rawMessage == null || rawMessage.isEmpty()) {
            return Component.empty();
        }
        
        // Mesaj anahtarı ve placeholder'lar için özel cache anahtarı oluştur
        StringBuilder cacheKeyBuilder = new StringBuilder("msg:" + key);
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            cacheKeyBuilder.append(":").append(entry.getKey()).append("=").append(entry.getValue());
        }
        String cacheKey = cacheKeyBuilder.toString();
        
        // Cache'ten kontrol et
        Component cachedMessage = messageCache.get(cacheKey);
        if (cachedMessage != null) {
            return cachedMessage;
        }
        
        // Yer tutucuları değiştir
        String processedMessage = rawMessage;
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            // % ile başlayan eski placeholderlar için
            processedMessage = processedMessage.replace("%" + entry.getKey() + "%", entry.getValue());
            // { } ile çevrili yeni placeholderlar için
            processedMessage = processedMessage.replace("{" + entry.getKey() + "}", entry.getValue());
        }
        
        // MiniMessage ile formatla
        Component formattedMessage = miniMessage.deserialize(processedMessage);
        
        // Cache'e kaydet
        messageCache.put(cacheKey, formattedMessage);
        checkCacheExpiry();
        
        return formattedMessage;
    }

    /**
     * Önbelleği temizleme kontrolü 
     */
    private void checkCacheExpiry() {
        long currentTime = System.currentTimeMillis();
        
        // Önbellek temizleme zamanı gelmiş mi kontrol et
        if (currentTime - lastCacheClear > CACHE_EXPIRY_MS) {
            // Asenkron olarak önbelleği temizle
            new BukkitRunnable() {
                @Override
                public void run() {
                    messageCache.clear();
                    cachedPrefix = null;
                    lastCacheClear = currentTime;
                }
            }.runTaskAsynchronously(plugin);
        }
    }

    /**
     * Ön eki getirir (önbellekli)
     * 
     * @return Ön ek bileşeni
     */
    public Component getPrefix() {
        if (cachedPrefix != null) {
            return cachedPrefix;
        }
        
        String prefix = config.getString("messages.prefix", "<dark_gray>[<aqua>cDuello<dark_gray>]</dark_gray> ");
        cachedPrefix = miniMessage.deserialize(prefix);
        return cachedPrefix;
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
     * String'i MiniMessage formatında formatlar.
     * Performans için önbellek kullanır.
     * 
     * @param input Formatlanacak string
     * @return Formatlanmış component
     */
    public Component format(String input) {
        if (input == null || input.isEmpty()) {
            return Component.empty();
        }
        
        // Cache'ten kontrol et
        String cacheKey = "fmt:" + input.hashCode();
        Component cachedFormat = messageCache.get(cacheKey);
        
        if (cachedFormat != null) {
            return cachedFormat;
        }
        
        // Cache'te yoksa, yeni formatı oluştur
        Component formatted = miniMessage.deserialize(input);
        
        // Cache'e kaydet (eğer çok uzun değilse - uzun stringler için önbellek kullanımı bellek sorunlarına yol açabilir)
        if (input.length() < 100) {
            messageCache.put(cacheKey, formatted);
            checkCacheExpiry();
        }
        
        return formatted;
    }

    /**
     * Tüm kaynakları serbest bırakır
     */
    public void close() {
        if (adventure != null) {
            adventure.close();
        }
    }

    /**
     * Mesaj yöneticisini yeniden yükler ve önbelleği temizler
     */
    public void reload() {
        // Mesaj önbelleğini temizle
        messageCache.clear();
        cachedPrefix = null;
        lastCacheClear = System.currentTimeMillis();
        
        // Debug mesajı yazdır
        if (plugin.getConfig().getBoolean("debug", false)) {
            plugin.getLogger().info("Mesaj yöneticisi yeniden yüklendi.");
        }
    }
} 