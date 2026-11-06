package net.onestorm.bend.client.api;

/**
 * Is told when one registered endpoint becomes usable and when it stops being usable.
 *
 * <p>A listener is given with the registration: to
 * {@link Client#registerOutgoing(NamespacedId, EndpointCodec, EndpointListener)} for an outgoing
 * endpoint, and as part of the {@link EndpointHandler} for an incoming one. It lives exactly as
 * long as the registration, so it can't miss the endpoint's first {@link #onAvailable} and needs no
 * removing. Both methods do nothing by default, so a listener overrides only the one it needs.
 *
 * <p>An endpoint is available when {@link EndpointHandle#isAvailable()} says so. Every registration
 * starts unavailable. The listener is then told each change, in order and one at a time:
 * {@code onAvailable} and {@code onUnavailable} alternate, starting with {@code onAvailable}. An
 * endpoint becomes available after each reconnect once its id is usable again, and when the server
 * starts offering its id at runtime; it becomes unavailable when the server stops offering it (for
 * an incoming endpoint, once the client has acknowledged that list, see
 * {@link EndpointHandle#isAvailable()}), when the client disconnects, and when the client is
 * closed. After
 * {@link EndpointHandle#unregister()} returns, the listener is not called again; a call already
 * running may still finish.
 *
 * <p>Each method gets the endpoint's handle, so a listener can use the endpoint even while it is
 * being created, for example to send a full state as soon as an outgoing endpoint is available:
 *
 * <pre>{@code
 * client.registerOutgoing(SYNC, syncCodec, new EndpointListener<OutgoingEndpoint<SyncState>>() {
 *     @Override
 *     public void onAvailable(OutgoingEndpoint<SyncState> sync) {
 *         sync.send(fullState());
 *     }
 * });
 * }</pre>
 *
 * <p>The endpoint can stop being usable again right after {@code onAvailable} is called, so
 * {@link OutgoingEndpoint#send(Object)} may still throw inside it. If a method throws, the
 * implementation reports the failure; the endpoint and the connection are not affected.
 *
 * @param <H> the type of the endpoint's handle, {@link IncomingEndpoint} or {@link OutgoingEndpoint}
 */
public interface EndpointListener<H extends EndpointHandle> {

    /**
     * Called when the endpoint has become available.
     *
     * @param endpoint the endpoint's handle
     */
    default void onAvailable(H endpoint) {
    }

    /**
     * Called when the endpoint has stopped being available.
     *
     * @param endpoint the endpoint's handle
     */
    default void onUnavailable(H endpoint) {
    }
}
