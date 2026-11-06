package net.onestorm.bend.client.api;

/**
 * Why a connection ended, as a type a caller can branch on.
 *
 * <p>The constants mirror the {@code Type} of the protocol's {@code Disconnect} packet
 * (protocol-v1, "Disconnect"), with the same ids. A disconnect the server did not send is given a
 * type too, see {@link DisconnectOrigin}, so one switch on the type works for every disconnect.
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
