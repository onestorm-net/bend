package net.onestorm.bend.client.api;

/**
 * The handle of an endpoint the client sends messages on.
 *
 * <p>Each message sent on the endpoint is encoded by its {@link EndpointCodec}. Created by
 * {@link Client#registerOutgoing(NamespacedId, EndpointCodec)}.
 *
 * @param <T> the type of message this endpoint sends
 */
public interface OutgoingEndpoint<T> extends EndpointHandle {
}
