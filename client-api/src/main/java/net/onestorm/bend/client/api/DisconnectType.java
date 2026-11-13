package net.onestorm.bend.client.api;

/**
 * Why a connection ended, as a type a caller can branch on.
 *
 * <p>The constants mirror the {@code Type} of the protocol's {@code Disconnect} packet
 * (protocol-v1, "Disconnect"), with the same ids. A disconnect the server did not send is given a
 * type too, so one switch on the type works for every disconnect:
 *
 * <ul>
 *   <li>The server sent a {@code Disconnect}: the type and reason the server sent. A type id this
 *       client does not know is reported as {@link #GENERIC}, with the server's reason kept.
 *   <li>The client gave up because the server broke a rule the protocol makes the client enforce
 *       (protocol-v1, "Client-side disconnect conditions"): {@link #TIMEOUT} when the server did
 *       not answer in time, {@link #PROTOCOL_ERROR} when it sent packets out of order, an invalid
 *       packet, or an endpoint message the client may not accept.
 *   <li>The connection could not be opened, was lost without a {@code Disconnect}, or was ended by
 *       {@link Client#close()}: {@link #GENERIC}, with a reason that describes the cause.
 * </ul>
 */
public enum DisconnectType {

    /**
     * Id {@code 0}: no more specific type applies; the reason says why.
     */
    GENERIC(0),

    /**
     * Id {@code 1}: a peer broke the protocol, for example by sending packets out of order or an
     * invalid packet.
     */
    PROTOCOL_ERROR(1),

    /**
     * Id {@code 2}: the server does not support the protocol version the client asked for.
     */
    UNSUPPORTED_VERSION(2),

    /**
     * Id {@code 3}: a peer did not answer in time, for example a missing {@code Keep Alive} or
     * handshake response.
     */
    TIMEOUT(3);

    private final int id;

    DisconnectType(int id) {
        this.id = id;
    }

    /**
     * Returns the id of this type in the protocol's {@code Disconnect} packet.
     *
     * @return the id, from {@code 0} to {@code 3}
     */
    public int getId() {
        return id;
    }

    /**
     * Returns the type with the given protocol id.
     *
     * @param id the id of a type in the protocol's {@code Disconnect} packet
     * @return the type with that id
     * @throws IllegalArgumentException if no type has that id
     */
    public static DisconnectType fromId(int id) {
        for (DisconnectType type : values()) {
            if (type.id == id) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown disconnect type id " + id);
    }
}
