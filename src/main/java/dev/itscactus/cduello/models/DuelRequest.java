package dev.itscactus.cduello.models;

import java.util.Objects;
import java.util.UUID;

/**
 * Represents a duel request from one player to another
 */
public class DuelRequest {
    private final UUID sender;
    private final UUID target;
    private final long timestamp;
    private double betAmount;

    /**
     * Create a new duel request
     *
     * @param sender The UUID of the player sending the request
     * @param target The UUID of the player receiving the request
     */
    public DuelRequest(UUID sender, UUID target) {
        this.sender = sender;
        this.target = target;
        this.timestamp = System.currentTimeMillis();
        this.betAmount = 0.0;
    }

    /**
     * Get the UUID of the player who sent the request
     *
     * @return The sender's UUID
     */
    public UUID getSender() {
        return sender;
    }

    /**
     * Get the UUID of the player who received the request
     *
     * @return The target's UUID
     */
    public UUID getTarget() {
        return target;
    }

    /**
     * Get the time the request was sent
     *
     * @return The timestamp
     */
    public long getTimestamp() {
        return timestamp;
    }

    public double getBetAmount() {
        return betAmount;
    }

    public void setBetAmount(double betAmount) {
        this.betAmount = betAmount;
    }

    public boolean isMoneyDuel() {
        return betAmount > 0;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DuelRequest that = (DuelRequest) o;
        return timestamp == that.timestamp &&
                Objects.equals(sender, that.sender) &&
                Objects.equals(target, that.target);
    }

    @Override
    public int hashCode() {
        return Objects.hash(sender, target, timestamp);
    }
} 