package net.onestorm.bend.client.api;

/**
 * Is told when a {@link Client} connects and disconnects.
 *
 * <p>A listener is added with {@link Client#addConnectionListener(ConnectionListener)}. Both
 * methods do nothing by default, so a listener overrides only the one it needs.
 *
 * <p>The client may connect several times, for example after reconnects. Every connection attempt
 * ends with exactly one {@link #onDisconnect(DisconnectInfo)}, also an attempt that never
 * connected, such as a refused connection or a server that rejects the protocol version.
 * {@link #onConnect()} is called in between only when the attempt connected. A listener gets one
 * client's events one at a time, in the order they happen.
 *
 * <p>A listener added while the client is already connected is not told about that connection's
 * {@code onConnect}. To learn the current state without missing an event, add the listener first
 * and then read {@link Client#isConnected()}.
 *
 * <p>If a method throws, the implementation reports the failure; the other listeners still get the
 * event, and the connection is not affected.
 */
public interface ConnectionListener {

    /**
     * Called when the client has connected: it acknowledged the server's first
     * {@code Available Endpoints} on this connection, so endpoints can be used once the server offers
     * them.
     */
    default void onConnect() {
    }

    /**
     * Called when a connection attempt ended, whether or not it connected.
     *
     * @param info why the connection ended, never {@code null}
     */
    default void onDisconnect(DisconnectInfo info) {
    }
}
