package dev.itscactus.cduello.models;

import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.UUID;

/**
 * Represents an active duel between two players
 */
public class Duel {
    private final UUID id;
    private final UUID challenger;
    private final UUID challenged;
    private Location challengerLocation;
    private Location challengedLocation;
    private DuelState state;
    private UUID winner;
    private double betAmount;
    private boolean moneyDuel;

    /**
     * Create a new duel
     *
     * @param id The UUID of the duel
     * @param challenger The challenger player
     * @param challenged The challenged player
     */
    public Duel(UUID id, Player challenger, Player challenged) {
        this.id = id;
        this.challenger = challenger.getUniqueId();
        this.challenged = challenged.getUniqueId();
        this.challengerLocation = challenger.getLocation();
        this.challengedLocation = challenged.getLocation();
        this.state = DuelState.PENDING;
        this.winner = null;
        this.betAmount = 0.0;
        this.moneyDuel = false;
    }

    /**
     * Create a new duel with specified locations and bet amount
     *
     * @param challenger The challenger player
     * @param challenged The challenged player
     * @param challengerLocation The location of the challenger
     * @param challengedLocation The location of the challenged
     * @param betAmount The amount of money bet on the duel
     */
    public Duel(Player challenger, Player challenged, Location challengerLocation, Location challengedLocation, double betAmount) {
        this.id = UUID.randomUUID();
        this.challenger = challenger.getUniqueId();
        this.challenged = challenged.getUniqueId();
        this.challengerLocation = challengerLocation;
        this.challengedLocation = challengedLocation;
        this.state = DuelState.PENDING;
        this.winner = null;
        this.betAmount = betAmount;
        this.moneyDuel = betAmount > 0;
    }

    /**
     * Get the UUID of the duel
     *
     * @return The UUID of the duel
     */
    public UUID getId() {
        return id;
    }

    /**
     * Get the UUID of the challenger
     *
     * @return The UUID of the challenger
     */
    public UUID getChallenger() {
        return challenger;
    }

    /**
     * Get the UUID of the challenged player
     *
     * @return The UUID of the challenged player
     */
    public UUID getChallenged() {
        return challenged;
    }

    /**
     * Get the location of the challenger before the duel
     *
     * @return The location of the challenger
     */
    public Location getChallengerLocation() {
        return challengerLocation != null ? challengerLocation.clone() : null;
    }

    /**
     * Set the location of the challenger
     *
     * @param location The new location of the challenger
     */
    public void setChallengerLocation(Location location) {
        this.challengerLocation = location != null ? location.clone() : null;
    }

    /**
     * Get the location of the challenged player before the duel
     *
     * @return The location of the challenged player
     */
    public Location getChallengedLocation() {
        return challengedLocation != null ? challengedLocation.clone() : null;
    }

    /**
     * Set the location of the challenged player
     *
     * @param location The new location of the challenged player
     */
    public void setChallengedLocation(Location location) {
        this.challengedLocation = location != null ? location.clone() : null;
    }

    /**
     * Get the state of the duel
     *
     * @return The state of the duel
     */
    public DuelState getState() {
        return state;
    }

    /**
     * Set the state of the duel
     *
     * @param state The new state of the duel
     */
    public void setState(DuelState state) {
        this.state = state;
    }

    /**
     * Get the UUID of the winner of the duel
     *
     * @return The UUID of the winner
     */
    public UUID getWinner() {
        return winner;
    }

    /**
     * Set the winner of the duel
     *
     * @param winner The UUID of the winner
     */
    public void setWinner(UUID winner) {
        this.winner = winner;
    }

    /**
     * Get the amount of money bet on the duel
     *
     * @return The amount of money bet
     */
    public double getBetAmount() {
        return betAmount;
    }

    /**
     * Set the amount of money bet on the duel
     *
     * @param betAmount The new amount of money bet
     */
    public void setBetAmount(double betAmount) {
        this.betAmount = betAmount;
        this.moneyDuel = betAmount > 0;
    }

    /**
     * Check if the duel is a money duel
     *
     * @return Whether the duel is a money duel
     */
    public boolean isMoneyDuel() {
        return moneyDuel;
    }

    /**
     * Get the total amount of money in the duel
     *
     * @return The total amount of money in the duel
     */
    public double getTotalPot() {
        return betAmount * 2;
    }

    /**
     * Get the amount of money a player would receive based on their percentage of the total pot
     *
     * @param percentage The percentage of the total pot
     * @return The amount of money the player would receive
     */
    public double getWinnerAmount(double percentage) {
        return getTotalPot() * (percentage / 100.0);
    }

    /**
     * Check if a player is in this duel
     *
     * @param playerUuid The UUID of the player to check
     * @return Whether the player is in this duel
     */
    public boolean hasPlayer(UUID playerUuid) {
        return challenger.equals(playerUuid) || challenged.equals(playerUuid);
    }

    /**
     * Get the UUID of the opponent of a player
     *
     * @param playerUuid The UUID of the player
     * @return The UUID of the opponent, or null if the player is not in this duel
     */
    public UUID getOpponent(UUID playerUuid) {
        if (challenger.equals(playerUuid)) {
            return challenged;
        } else if (challenged.equals(playerUuid)) {
            return challenger;
        }
        return null;
    }

    /**
     * Enum representing the state of a duel
     */
    public enum DuelState {
        PENDING,
        ACTIVE,
        FINISHED,
        CANCELLED
    }
} 