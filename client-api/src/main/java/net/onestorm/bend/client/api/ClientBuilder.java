package net.onestorm.bend.client.api;

/**
 * Builds and connects one {@link Client}.
 *
 * <p>An implementation's builder implements this interface and adds its own settings, such as the
 * server address. Code that is handed a {@code ClientBuilder} can add listeners and connect without
 * knowing the implementation. An implementation may narrow the return type of
 * {@link #addListener(ClientListener)} to its own builder, so its settings still chain after it.
 *
 * <p>One builder makes one client: after {@link #connect()} the builder can't be used again. A
 * builder is meant to be used by one thread; it is not safe to share between threads.
 */
public interface ClientBuilder {

    /**
     * Adds a listener that is told about events of the client this builder makes.
     *
     * <p>The listener is in place before the first connection attempt starts, so it gets that
     * attempt's {@link ClientListener#onConnect()} and
     * {@link ClientListener#onDisconnect(DisconnectInfo)}, which a listener added with
     * {@link Client#addListener(ClientListener)} after {@link #connect()} may miss. Once the client exists, the listener is one of its listeners as
     * if added with {@link Client#addListener(ClientListener)}, and
     * {@link Client#removeListener(ClientListener)} removes it. Adding a listener that is already
     * added does nothing.
     *
     * @param listener the listener to add
     * @return this builder
     * @throws NullPointerException if {@code listener} is {@code null}
     * @throws IllegalStateException if {@link #connect()} was already called
     */
    ClientBuilder addListener(ClientListener listener);

    /**
     * Creates the client and starts its first connection attempt.
     *
     * <p>This method returns at once with a client that is connecting; it does not wait for the
     * connection, see {@link Client#whenReady()}. A failed attempt does not make this method throw:
     * it reaches the listeners as {@link ClientListener#onDisconnect(DisconnectInfo)}, and the
     * client keeps trying or closes, as its implementation decides.
     *
     * @return the new client
     * @throws IllegalStateException if this method was already called
     */
    Client connect();
}
