package net.onestorm.bend.client.api;

/**
 * Thrown by {@link OutgoingEndpoint#send(Object)} when the endpoint can't be used right now.
 *
 * <p>It reports the state of the endpoint or the connection, such as an endpoint the server doesn't
 * offer, a client that is disconnected or a client that is closed, not a misuse of the handle: a
 * send on an unregistered handle throws a plain {@link IllegalStateException} instead. Most of these
 * states pass while the client reconnects, so a caller may catch this exception and drop or retry
 * the message; once the client is closed, a retry fails the same way.
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
