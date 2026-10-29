package net.onestorm.bend.client.api;

/**
 * A client of a Bend server.
 *
 * <p>A client is created by an implementation's builder, which returns this interface. It has no
 * {@code connect()} and no settings, so it can be shared with code that must not reconfigure it or
 * connect it again.
 *
 * <p>Endpoints are registered here. A registration belongs to the client, not to one connection,
 * and survives reconnects: after a reconnect the client declares the same endpoints again.
 * Registering an id once as incoming and once as outgoing makes a bidirectional endpoint.
 * Registering does not wait for the server: an endpoint can be used once the server offers its id.
 *
 * <p>Endpoint ids are usually declared once, as constants, so an invalid id fails where it is
 * defined:
 *
 * <pre>{@code
 * static final NamespacedId CHAT = NamespacedId.of("bend:chat");
 *
 * OutgoingEndpoint<ChatMessage> chat = client.registerOutgoing(CHAT, chatCodec);
 * }</pre>
 */
public interface Client {

    /**
     * Registers an endpoint the client sends messages on.
     *
     * @param <T> the type of message the endpoint sends
     * @param id the endpoint's id
     * @param codec the codec that encodes the messages sent on the endpoint
     * @return the handle of the new registration
     * @throws NullPointerException if {@code id} or {@code codec} is {@code null}
     * @throws IllegalStateException if {@code id} is already registered as outgoing on this client
     */
    <T> OutgoingEndpoint<T> registerOutgoing(NamespacedId id, EndpointCodec<T> codec);

    /**
     * Registers an endpoint the client receives messages on.
     *
     * @param <T> the type of message the endpoint receives
     * @param id the endpoint's id
     * @param codec the codec that decodes the messages received on the endpoint
     * @param handler the handler that receives each decoded message
     * @return the handle of the new registration
     * @throws NullPointerException if {@code id}, {@code codec} or {@code handler} is {@code null}
     * @throws IllegalStateException if {@code id} is already registered as incoming on this client
     */
    <T> IncomingEndpoint<T> registerIncoming(NamespacedId id, EndpointCodec<T> codec,
            EndpointHandler<T> handler);
}
