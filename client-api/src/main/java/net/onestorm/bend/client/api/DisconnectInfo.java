package net.onestorm.bend.client.api;

import java.util.Objects;

/**
 * Why a connection ended: its {@link DisconnectType} and a reason.
 *
 * <p>Given to {@link ClientListener#onDisconnect(DisconnectInfo)}. Branch on the type; the reason
 * is free text for logging and is not safe to match on.
 *
 * <p>Instances are immutable and safe to share between threads. Two infos are equal when their
 * type and reason are equal.
 */
public final class DisconnectInfo {

    private final DisconnectType type;
    private final String reason;

    private DisconnectInfo(DisconnectType type, String reason) {
        this.type = type;
        this.reason = reason;
    }

    /**
     * Creates the info of one disconnect.
     *
     * @param type the type of the disconnect
     * @param reason the reason, possibly empty
     * @return the info
     * @throws NullPointerException if {@code type} or {@code reason} is {@code null}
     */
    public static DisconnectInfo of(DisconnectType type, String reason) {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(reason, "reason");
        return new DisconnectInfo(type, reason);
    }

    /**
     * Returns the type of the disconnect.
     *
     * @return the type
     */
    public DisconnectType getType() {
        return type;
    }

    /**
     * Returns the reason, free text for logging; when the server sent a {@code Disconnect}, the
     * reason the server sent.
     *
     * @return the reason, possibly empty, never {@code null}
     */
    public String getReason() {
        return reason;
    }

    /**
     * Returns whether {@code other} is a {@code DisconnectInfo} with the same type and reason.
     *
     * @param other the object to compare with
     * @return {@code true} if both infos have the same type and reason
     */
    @Override
    public boolean equals(Object other) {
        if (!(other instanceof DisconnectInfo that)) {
            return false;
        }
        return type == that.type && reason.equals(that.reason);
    }

    /**
     * Returns a hash code based on the type and reason.
     *
     * @return the hash code
     */
    @Override
    public int hashCode() {
        return Objects.hash(type, reason);
    }

    /**
     * Returns the type and reason, for logging.
     *
     * @return a description of this disconnect
     */
    @Override
    public String toString() {
        return "DisconnectInfo{type=" + type + ", reason=\"" + reason + "\"}";
    }
}
