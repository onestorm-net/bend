package net.onestorm.bend.client.api;

import java.nio.ByteBuffer;

/**
 * Converts the messages of one endpoint to and from the bytes of their payload.
 *
 * <p>Each message on an endpoint is sent as a payload of bytes whose format belongs to the
 * endpoint's codec; Bend itself never reads it. A codec decodes the payloads that arrive on an
 * incoming endpoint and encodes the messages sent on an outgoing endpoint. A codec is given to
 * {@link Client#registerIncoming(NamespacedId, EndpointCodec, EndpointHandler)} or
 * {@link Client#registerOutgoing(NamespacedId, EndpointCodec)}.
 *
 * <p>Both directions avoid copying: {@link #decode(ByteBuffer)} reads the received bytes in place,
 * and the buffer {@link #encode(Object)} returns is sent as it is. A codec that keeps or changes a
 * buffer against the rules on these methods corrupts data without any error, so follow them
 * exactly.
 *
 * @param <T> the type of message this codec converts
 */
public interface EndpointCodec<T> {

    /**
     * Decodes one received payload into a message.
     *
     * <p>The payload is the bytes between the buffer's position and its limit. They were chosen by
     * the peer, so treat them as untrusted: check any length read from them before it sizes an
     * allocation. The buffer is read-only and starts in big-endian order. Bytes left unread are
     * ignored.
     *
     * <p>The buffer is only valid during this call. Neither the codec nor the returned message may
     * keep it, or anything that refers to it such as a slice, duplicate or view, after the call
     * returns; the message has to hold its own copy of any bytes it needs.
     *
     * <p>If this method throws or returns {@code null}, the message is lost and the implementation
     * reports the failure. The connection stays open: a payload that can't be decoded is an
     * application error, not a protocol violation.
     *
     * @param payload the received payload, read-only and valid only during this call
     * @return the decoded message, never {@code null}
     */
    T decode(ByteBuffer payload);

    /**
     * Encodes a message into the payload to send.
     *
     * <p>The bytes between the returned buffer's position and its limit are sent as the payload.
     * Any kind of buffer may be returned (heap, direct or read-only), and an empty buffer sends an
     * empty payload.
     *
     * <p>The returned buffer is handed over to Bend, which may send it later: after returning it,
     * the codec must not change it or its contents. Return a new buffer for each message rather
     * than reusing one.
     *
     * <p>If this method throws or returns {@code null}, the future returned by
     * {@link OutgoingEndpoint#send(Object)} fails and nothing is written.
     *
     * @param message the message to encode
     * @return the payload to send, never {@code null}
     */
    ByteBuffer encode(T message);
}
