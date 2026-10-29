package net.onestorm.bend.client.api;

/**
 * The handle of an endpoint the client receives messages on.
 *
 * <p>Each message that arrives on the endpoint is decoded by its {@link EndpointCodec} and passed to
 * its {@link EndpointHandler}. Created by {@link Client#registerIncoming(NamespacedId,
 * EndpointCodec, EndpointHandler)}.
 *
 * @param <T> the type of message this endpoint receives
 */
public interface IncomingEndpoint<T> extends EndpointHandle {
}
