package net.onestorm.bend.client.api;

/**
 * Thrown by {@link OutgoingEndpoint#send(Object)} when the endpoint can't be used right now.
 *
 * <p>It reports a state of the connection that a client which reconnects passes through, such as
 * an endpoint the server doesn't offer or a client that is disconnected, not a bug in the caller's
 * code. A caller may catch it and drop or retry the message.
 */
public class EndpointUnavailableException extends IllegalStateException {

    /**
     * Creates the exception.
     *
     * @param message the detail message, naming the endpoint and why it is unavailable
     */
    public EndpointUnavailableException(String message) {
        super(message);
    }
}
