package net.onestorm.bend.client.api;

/**
 * The handle of an endpoint the client sends messages on.
 *
 * <p>Messages are sent with {@link #send(Object)}, and each one is encoded by the endpoint's
 * {@link EndpointCodec}. Created by {@link Client#registerOutgoing(NamespacedId, EndpointCodec)}.
 *
 * @param <T> the type of message this endpoint sends
 */
public interface OutgoingEndpoint<T> extends EndpointHandle {

    /**
     * Sends one message on this endpoint.
     *
     * <p>The endpoint's codec encodes the message, and the payload is sent to the server in an
     * {@code Endpoint Message}. Only this endpoint's messages are sent this way; the protocol's own
     * packets never go through this method.
     *
     * <p>Returning means the message was accepted, not that it was written or that the server
     * received it: the protocol has no acknowledgement for a single message. An accepted message
     * may still be dropped, for example when the endpoint or the connection stops being usable
     * before it is written, or when the write fails; this method does not report it.
     *
     * @param message the message to send
     * @throws NullPointerException if {@code message} is {@code null}
     * @throws IllegalStateException if this handle has been unregistered, the server does not
     *     currently offer this endpoint's id, the server's flags for it don't allow the client to
     *     send on it, the server has not yet acknowledged the {@code Supported Endpoints} that
     *     declares it, the client is disconnected, or the client is closed; the message names the
     *     reason
     */
    void send(T message);
}
