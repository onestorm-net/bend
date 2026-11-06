package net.onestorm.bend.client.api;

/**
 * Which side ended a connection, or what ended it.
 *
 * <p>Each origin says which {@link DisconnectType} a {@link DisconnectInfo} with that origin
 * carries.
 */
public enum DisconnectOrigin {

    /**
     * The server sent a {@code Disconnect}. The type and reason are the ones the server sent; a
     * type id this client does not know is reported as {@link DisconnectType#GENERIC}, with the
     * server's reason kept.
     */
    SERVER,

    /**
     * The client gave up on the connection because the server broke a rule the protocol makes the
     * client enforce (protocol-v1, "Client-side disconnect conditions"): the type is
     * {@link DisconnectType#TIMEOUT} when the server did not answer in time, and
     * {@link DisconnectType#PROTOCOL_ERROR} when it sent packets out of order, an invalid packet,
     * or an endpoint message the client may not accept.
     */
    CLIENT,

    /**
     * The connection could not be opened, or it was lost without a {@code Disconnect}, for example
     * because it was refused, reset or closed by the server. The type is
     * {@link DisconnectType#GENERIC} and the reason describes the cause.
     */
    CONNECTION,

    /**
     * {@link Client#close()} ended a connection attempt that was in progress or connected. The type
     * is {@link DisconnectType#GENERIC}. When the implementation stops trying on its own, the last
     * attempt has already ended with its own origin, so there is no {@code CLOSED} disconnect.
     */
    CLOSED
}
