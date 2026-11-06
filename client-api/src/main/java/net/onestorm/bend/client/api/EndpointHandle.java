package net.onestorm.bend.client.api;

/**
 * A handle to one registration of an endpoint on a {@link Client}.
 *
 * <p>A handle is returned by {@link Client#registerIncoming(NamespacedId, EndpointCodec,
 * EndpointHandler)} or {@link Client#registerOutgoing(NamespacedId, EndpointCodec)}. It belongs to
 * the client, not to one connection, so it stays valid when the client reconnects.
 */
public interface EndpointHandle {

    /**
     * Returns the id this endpoint was registered with.
     *
     * @return the endpoint's id
     */
    NamespacedId getId();

    /**
     * Returns whether this endpoint can be used right now.
     *
     * <p>An outgoing endpoint is available when the client is connected, the server offers its id
     * with flags that let the client send on it, and the server has acknowledged the
     * {@code Supported Endpoints} that declares it: exactly when
     * {@link OutgoingEndpoint#send(Object)} does not refuse a message because of the endpoint's or
     * the connection's state. An incoming endpoint is available when the client is connected, the
     * server offers its id with flags that let the server send on it, and the server has
     * acknowledged the {@code Supported Endpoints} that declares it (protocol-v1, "Endpoint list
     * updates"). An unregistered endpoint, and every endpoint of a closed client, is not available.
     *
     * <p>When the server stops offering an id, an outgoing endpoint becomes unavailable as soon as
     * the client receives the server's new list, because the client must stop sending on it before
     * acknowledging that list. An incoming endpoint becomes unavailable only once the client has
     * acknowledged that list: messages the server sent before the acknowledgement are still valid
     * and are still given to the handler (protocol-v1, "Endpoint list updates").
     *
     * <p>The result is the latest state, from whichever thread calls this method, but it may change
     * right after this method returns. To act on changes, give the registration an
     * {@link EndpointListener}.
     *
     * @return {@code true} if this endpoint is available
     */
    boolean isAvailable();

    /**
     * Removes this registration from its client.
     *
     * <p>For an incoming endpoint, its handler is not given messages that arrive after this call;
     * a handler call already running may still finish. Once the id has no other registration on
     * the client, the client stops declaring it supported to the server. A registration of the
     * same id in the other direction is not affected.
     *
     * <p>Afterwards the same id may be registered again in the same direction, which returns a new
     * handle. Calling this method more than once does nothing after the first call, and never
     * affects a newer registration of the same id. After the client is closed, this method does
     * nothing.
     */
    void unregister();
}
