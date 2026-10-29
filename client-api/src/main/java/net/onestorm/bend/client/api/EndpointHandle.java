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
     * Removes this registration from its client.
     *
     * <p>For an incoming endpoint, its handler receives no further messages. Once the id has no
     * other registration on the client, the client stops declaring it supported to the server. A
     * registration of the same id in the other direction is not affected.
     *
     * <p>Afterwards the same id may be registered again in the same direction, which returns a new
     * handle. Calling this method more than once does nothing after the first call, and never
     * affects a newer registration of the same id.
     */
    void unregister();
}
