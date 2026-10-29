package net.onestorm.bend.client.api;

/**
 * Receives the messages that arrive on one incoming endpoint.
 *
 * <p>Each received payload is first decoded by the endpoint's {@link EndpointCodec}; the handler
 * gets the decoded message. A handler is given to
 * {@link Client#registerIncoming(NamespacedId, EndpointCodec, EndpointHandler)}.
 *
 * @param <T> the type of message this handler receives
 */
@FunctionalInterface
public interface EndpointHandler<T> {

    /**
     * Handles one received message.
     *
     * <p>Called once for each message the endpoint's codec decoded, never with {@code null}. If
     * this method throws, that message is lost and the implementation reports the failure. The
     * connection stays open: a failing handler is an application error, not a protocol
     * violation.
     *
     * @param message the decoded message
     */
    void handle(T message);
}
