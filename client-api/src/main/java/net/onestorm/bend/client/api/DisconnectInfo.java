package net.onestorm.bend.client.api;

import java.util.Objects;

/**
 * Why a connection ended: its {@link DisconnectOrigin}, its {@link DisconnectType} and a reason.
 *
 * <p>Given to {@link ClientListener#onDisconnect(DisconnectInfo)}. Branch on the origin and
 * the type; the reason is free text for logging and is not safe to match on.
 *
 * <p>Instances are immutable and safe to share between threads. Two infos are equal when their
 * origin, type and reason are equal.
 */
public final class DisconnectInfo {

    private final DisconnectOrigin origin;
    private final DisconnectType type;
    private final String reason;

    private DisconnectInfo(DisconnectOrigin origin, DisconnectType type, String reason) {
        this.origin = origin;
        this.type = type;
        this.reason = reason;
    }

    /**
     * Creates the info of one disconnect.
     *
     * @param origin which side ended the connection, or what ended it
     * @param type the type of the disconnect
     * @param reason the reason, possibly empty
     * @return the info
     * @throws NullPointerException if {@code origin}, {@code type} or {@code reason} is
     *         {@code null}
     */
    public static DisconnectInfo of(DisconnectOrigin origin, DisconnectType type, String reason) {
        Objects.requireNonNull(origin, "origin");
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(reason, "reason");
        return new DisconnectInfo(origin, type, reason);
    }

    /**
     * Returns which side ended the connection, or what ended it.
     *
     * @return the origin
     */
    public DisconnectOrigin getOrigin() {
        return origin;
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
     * Returns the reason, free text for logging; for a {@link DisconnectOrigin#SERVER} disconnect,
     * the reason the server sent.
     *
     * @return the reason, possibly empty, never {@code null}
     */
    public String getReason() {
        return reason;
    }

    /**
     * Returns whether {@code other} is a {@code DisconnectInfo} with the same origin, type and
     * reason.
     *
     * @param other the object to compare with
     * @return {@code true} if both infos have the same origin, type and reason
     */
    @Override
    public boolean equals(Object other) {
        if (!(other instanceof DisconnectInfo that)) {
            return false;
        }
        return origin == that.origin && type == that.type && reason.equals(that.reason);
    }

    /**
     * Returns a hash code based on the origin, type and reason.
     *
     * @return the hash code
     */
    @Override
    public int hashCode() {
        return Objects.hash(origin, type, reason);
    }

    /**
     * Returns the origin, type and reason, for logging.
     *
     * @return a description of this disconnect
     */
    @Override
    public String toString() {
        return "DisconnectInfo{origin=" + origin + ", type=" + type + ", reason=\"" + reason + "\"}";
    }
}
