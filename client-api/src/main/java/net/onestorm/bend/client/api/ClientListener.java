package net.onestorm.bend.client.api;

/**
 * Is told about events of a whole {@link Client}: for now, when it connects and disconnects.
 *
 * <p>A listener is added with {@link ClientBuilder#addListener(ClientListener)} before the client
 * connects, so it hears the first connection attempt, or with
 * {@link Client#addListener(ClientListener)} afterwards. Every method does nothing by default, so a
 * listener overrides only the ones it needs, and a later event of the client is a new method here
 * that existing listeners can ignore.
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
public interface ClientListener {

    /**
     * Called when the client has connected: it acknowledged the server's first
     * {@code Available Endpoints} on this connection. Endpoints are not usable yet at this point:
     * the client's own {@code Supported Endpoints} still has to be acknowledged.
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
