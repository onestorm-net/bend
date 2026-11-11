package net.onestorm.bend.client.api;

import java.util.concurrent.CompletionStage;

/**
 * A client of a Bend server.
 *
 * <p>A client is created by an implementation's builder, which implements {@link ClientBuilder}
 * and returns this interface. It has no {@code connect()} and no settings, so it can be shared with
 * code that must not reconfigure it or connect it again.
 *
 * <p>Endpoints are registered here. A registration belongs to the client, not to one connection,
 * and survives reconnects: after a reconnect the client declares the same endpoints again.
 * Registering an id once as incoming and once as outgoing makes a bidirectional endpoint.
 * Registering does not wait for the server: an endpoint can be used once the server offers its id.
 *
 * <p>Endpoint ids are usually declared once, as constants, so an invalid id fails where it is
 * defined:
 *
 * <pre>{@code
 * static final NamespacedId CHAT = NamespacedId.of("bend:chat");
 *
 * OutgoingEndpoint<ChatMessage> chat = client.registerOutgoing(CHAT, chatCodec);
 * chat.send(new ChatMessage("hello"));
 * }</pre>
 *
 * <p>The client is connected once it has acknowledged the server's first
 * {@code Available Endpoints} on the current connection; {@link #isConnected()} reports this, and
 * a {@link ClientListener} is told each time it connects and disconnects, including after
 * reconnects. Between connections the client is not connected. Once it will not connect again,
 * because {@link #close()} was called or because the implementation stopped trying, the client is
 * closed for good. A client is therefore either still trying to connect or closed.
 *
 * <p>A listener added on the {@link ClientBuilder} hears the first connection attempt too:
 *
 * <pre>{@code
 * Client client = builder
 *         .addListener(new ClientListener() {
 *             @Override
 *             public void onDisconnect(DisconnectInfo info) {
 *                 log.info("disconnected: " + info);
 *             }
 *         })
 *         .connect();
 * client.whenReady().toCompletableFuture().get(10, TimeUnit.SECONDS);
 * }</pre>
 */
public interface Client extends AutoCloseable {

    /**
     * Registers an endpoint the client sends messages on.
     *
     * @param <T> the type of message the endpoint sends
     * @param id the endpoint's id
     * @param codec the codec that encodes the messages sent on the endpoint
     * @return the handle of the new registration
     * @throws NullPointerException if {@code id} or {@code codec} is {@code null}
     * @throws IllegalStateException if {@code id} is already registered as outgoing on this client,
     *     or the client is closed
     */
    <T> OutgoingEndpoint<T> registerOutgoing(NamespacedId id, EndpointCodec<T> codec);

    /**
     * Registers an endpoint the client receives messages on.
     *
     * @param <T> the type of message the endpoint receives
     * @param id the endpoint's id
     * @param codec the codec that decodes the messages received on the endpoint
     * @param handler the handler that receives each decoded message
     * @return the handle of the new registration
     * @throws NullPointerException if {@code id}, {@code codec} or {@code handler} is {@code null}
     * @throws IllegalStateException if {@code id} is already registered as incoming on this client,
     *     or the client is closed
     */
    <T> IncomingEndpoint<T> registerIncoming(NamespacedId id, EndpointCodec<T> codec,
            EndpointHandler<T> handler);

    /**
     * Returns whether the client is connected right now.
     *
     * <p>The client is connected once it has sent {@code Available Endpoints Acknowledged} for the
     * server's first {@code Available Endpoints} on the current connection (protocol-v1, "Flow",
     * step 3). It is not connected before that, between connections, and after it is closed. The
     * result is the latest state, from whichever thread calls this method, but it may change right
     * after this method returns.
     *
     * @return {@code true} if the client is connected
     */
    boolean isConnected();

    /**
     * Returns a stage that completes when the client is ready: connected for the first time.
     *
     * <p>The stage completes normally the first time the client connects and stays completed, also
     * after later disconnects. If the client is closed before it ever connected, the stage
     * completes exceptionally with an {@link IllegalStateException} whose message names the last
     * disconnect, or says that the client was closed before any connection attempt ended. Since a
     * client either keeps trying or closes, the stage always completes in the end, though possibly
     * much later; use a timeout when waiting on it. A caller can't complete the stage for other
     * callers.
     *
     * <p>Ready means connected, not that a given endpoint can be used: the server may not offer it
     * yet, and the client's own {@code Supported Endpoints} may not be acknowledged yet.
     *
     * <p>To block until ready, call {@code whenReady().toCompletableFuture().get(timeout, unit)} on
     * a thread of your own. Callbacks chained on the stage may run on the implementation's I/O
     * thread, so they must not block.
     *
     * @return the stage of the first connection
     */
    CompletionStage<Void> whenReady();

    /**
     * Adds a listener that is told about events of the whole client, such as connecting and
     * disconnecting.
     *
     * <p>Adding a listener that is already added does nothing. The listener is not told about a
     * connection that was already made when it was added; see {@link ClientListener}. To hear the
     * first connection attempt, add the listener with
     * {@link ClientBuilder#addListener(ClientListener)} instead.
     *
     * @param listener the listener to add
     * @throws NullPointerException if {@code listener} is {@code null}
     * @throws IllegalStateException if the client is closed
     */
    void addListener(ClientListener listener);

    /**
     * Removes a listener added with {@link #addListener(ClientListener)}.
     *
     * <p>Removing a listener that is not added does nothing, also after the client is closed.
     *
     * @param listener the listener to remove
     * @throws NullPointerException if {@code listener} is {@code null}
     */
    void removeListener(ClientListener listener);

    /**
     * Closes the client for good.
     *
     * <p>The client disconnects and never connects again. After this method returns, registering
     * an endpoint and adding a listener throw {@link IllegalStateException},
     * {@link OutgoingEndpoint#send(Object)} throws it too, and {@link EndpointHandle#unregister()}
     * does nothing. A message that arrived but was not yet given to its handler is dropped; a
     * handler call already running may still finish. If a connection attempt was in progress or
     * connected, listeners get its final {@link ClientListener#onDisconnect(DisconnectInfo)},
     * with type {@link DisconnectType#GENERIC}, possibly after this method returns.
     *
     * <p>This method does not wait for the connection to close, so it can be called from an
     * endpoint handler or a connection listener. Calling it again does nothing. The client sends no
     * packet to the server: the protocol has no client-side {@code Disconnect}.
     */
    @Override
    void close();
}
