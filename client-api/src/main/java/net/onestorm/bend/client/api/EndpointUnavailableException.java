package net.onestorm.bend.client.api;

/**
 * The exception a send's future fails with when the endpoint can't be used for that message.
 *
 * <p>It reports a normal state of the connection, such as an endpoint the server doesn't offer or a
 * client that is disconnected, not a bug in the caller's code. That is why
 * {@link OutgoingEndpoint#send(Object)} never throws it, but fails its future with it instead.
 */
public class EndpointUnavailableException extends RuntimeException {

    /**
     * Creates the exception.
     *
     * @param message the detail message, naming the endpoint and why it is unavailable
     */
    public EndpointUnavailableException(String message) {
        super(message);
    }
}
