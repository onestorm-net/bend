package net.onestorm.bend.client.api;

import java.util.concurrent.CompletableFuture;

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
     * <p>This method returns without waiting for the write. The returned future completes normally
     * once the message has been written to the connection. That does not mean the server received or
     * handled it: the protocol has no acknowledgement for a single message.
     *
     * <p>The caller may ignore the future, in which case a failure is dropped, or attach callbacks to
     * it. The future completes once the message is written or the send fails; a write the connection
     * does not take stays pending until the connection closes, and then fails. Completing or
     * cancelling the returned future does not affect the send.
     *
     * <p>The future fails with an {@link EndpointUnavailableException} when this endpoint can't be
     * used for the message: the server does not currently offer its id, the server's flags for it
     * don't allow the client to send on it, the server has not yet acknowledged the client declaring
     * it, the client is disconnected or closed, or this handle is unregistered before the message is
     * written.
     *
     * <p>If the codec's {@link EndpointCodec#encode(Object)} throws, the future fails with that
     * exception; if it returns {@code null}, with a {@link NullPointerException}. Nothing is written
     * in either case. If writing fails, the future fails with the exception that caused it; the type
     * of that exception is not specified.
     *
     * @param message the message to send
     * @return a future that completes once the message is written
     * @throws NullPointerException if {@code message} is {@code null}
     * @throws IllegalStateException if this handle has been unregistered
     */
    CompletableFuture<Void> send(T message);
}
