package dev.itscactus.cduello.models;

import java.util.UUID;

/**
 * Represents the duel statistics for a player
 */
public class PlayerStats {
    private final UUID playerUuid;
    private int wins;
    private int losses;
    private double moneyWon;
    private double moneyLost;

    /**
     * Create new player stats
     *
     * @param playerUuid The UUID of the player
     */
    public PlayerStats(UUID playerUuid) {
        this.playerUuid = playerUuid;
        this.wins = 0;
        this.losses = 0;
        this.moneyWon = 0.0;
        this.moneyLost = 0.0;
    }

    /**
     * Create new player stats with existing data
     *
     * @param playerUuid The UUID of the player
     * @param wins The number of wins
     * @param losses The number of losses
     */
    public PlayerStats(UUID playerUuid, int wins, int losses) {
        this.playerUuid = playerUuid;
        this.wins = wins;
        this.losses = losses;
        this.moneyWon = 0.0;
        this.moneyLost = 0.0;
    }

    /**
     * Get the UUID of the player
     *
     * @return The player's UUID
     */
    public UUID getPlayerUuid() {
        return playerUuid;
    }

    /**
     * Get the number of wins
     *
     * @return The wins
     */
    public int getWins() {
        return wins;
    }

    /**
     * Set the number of wins
     *
     * @param wins The wins
     */
    public void setWins(int wins) {
        this.wins = wins;
    }

    /**
     * Increment the number of wins
     */
    public void incrementWins() {
        this.wins++;
    }

    /**
     * Get the number of losses
     *
     * @return The losses
     */
    public int getLosses() {
        return losses;
    }

    /**
     * Set the number of losses
     *
     * @param losses The losses
     */
    public void setLosses(int losses) {
        this.losses = losses;
    }

    /**
     * Increment the number of losses
     */
    public void incrementLosses() {
        this.losses++;
    }

    /**
     * Get the total number of duels
     *
     * @return The total duels
     */
    public int getTotalDuels() {
        return wins + losses;
    }

    /**
     * Get the win ratio
     *
     * @return The win ratio (0-1)
     */
    public double getWinRatio() {
        int total = getTotalDuels();
        return total > 0 ? (double) wins / total : 0;
    }

    public double getMoneyWon() {
        return moneyWon;
    }

    public double getMoneyLost() {
        return moneyLost;
    }

    public double getNetEarnings() {
        return moneyWon - moneyLost;
    }

    public void addWin() {
        wins++;
    }

    public void addLoss() {
        losses++;
    }

    public void addMoneyWon(double amount) {
        this.moneyWon += amount;
    }

    public void addMoneyLost(double amount) {
        this.moneyLost += amount;
    }
} 