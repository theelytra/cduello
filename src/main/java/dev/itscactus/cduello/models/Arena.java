package dev.itscactus.cduello.models;

import org.bukkit.Location;
import org.bukkit.World;

/**
 * Bir düello arenasını temsil eder
 */
public class Arena {
    private String id;
    private String name;
    private String worldName;
    private Location pos1;
    private Location pos2;
    private boolean enabled;

    /**
     * Yeni bir arena oluşturur
     *
     * @param id Arena ID'si
     * @param name Arena adı
     * @param world Arena dünyası
     * @param pos1 Birinci konum
     * @param pos2 İkinci konum
     */
    public Arena(String id, String name, World world, Location pos1, Location pos2) {
        this.id = id;
        this.name = name;
        this.worldName = world.getName();
        this.pos1 = pos1.clone();
        this.pos2 = pos2.clone();
        this.enabled = true;
    }

    /**
     * Yapılandırma bölümünden bir arena oluşturur
     *
     * @param id Arena ID'si
     * @param name Arena adı
     * @param worldName Dünya adı
     * @param pos1 Birinci konum
     * @param pos2 İkinci konum
     */
    public Arena(String id, String name, String worldName, Location pos1, Location pos2) {
        this.id = id;
        this.name = name;
        this.worldName = worldName;
        this.pos1 = pos1.clone();
        this.pos2 = pos2.clone();
        this.enabled = true;
    }

    /**
     * Arena ID'sini alır
     *
     * @return Arena ID'si
     */
    public String getId() {
        return id;
    }

    /**
     * Arena adını alır
     *
     * @return Arena adı
     */
    public String getName() {
        return name;
    }

    /**
     * Arena adını ayarlar
     *
     * @param name Yeni arena adı
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Arena dünyasının adını alır
     *
     * @return Dünya adı
     */
    public String getWorldName() {
        return worldName;
    }

    /**
     * Birinci konumu alır
     *
     * @return Birinci konum
     */
    public Location getPos1() {
        return pos1.clone();
    }

    /**
     * Birinci konumu ayarlar
     *
     * @param pos1 Yeni birinci konum
     */
    public void setPos1(Location pos1) {
        this.pos1 = pos1.clone();
    }

    /**
     * İkinci konumu alır
     *
     * @return İkinci konum
     */
    public Location getPos2() {
        return pos2.clone();
    }

    /**
     * İkinci konumu ayarlar
     *
     * @param pos2 Yeni ikinci konum
     */
    public void setPos2(Location pos2) {
        this.pos2 = pos2.clone();
    }

    /**
     * Arenanın etkin olup olmadığını kontrol eder
     *
     * @return Etkinse true, değilse false
     */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Arenanın etkinliğini ayarlar
     *
     * @param enabled Etkin olup olmadığı
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
} 