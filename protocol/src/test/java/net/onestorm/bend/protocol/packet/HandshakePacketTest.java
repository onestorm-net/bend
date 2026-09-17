package net.onestorm.bend.protocol.packet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.onestorm.bend.protocol.exception.ProtocolException;
import org.junit.jupiter.api.Test;

class HandshakePacketTest {

    @Test
    void handshake_roundTrip() {
        HandshakePacket original = new HandshakePacket(1, "test:client");
        HandshakePacket decoded = encodeAndDecode(original);

        assertThat(decoded.getProtocolVersionNumber()).isEqualTo(1);
        assertThat(decoded.getClientIdentifier()).isEqualTo("test:client");
    }

    @Test
    void handshake_maxLengthIdentifier_accepted() {
        String identifier = "a".repeat(64);
        HandshakePacket original = new HandshakePacket(1, identifier);
        HandshakePacket decoded = encodeAndDecode(original);

        assertThat(decoded.getClientIdentifier()).isEqualTo(identifier);
    }

    @Test
    void handshake_overMaxLengthIdentifier_throws() {
        HandshakePacket original = new HandshakePacket(1, "a".repeat(65));
        ByteBuf buf = Unpooled.buffer();
        try {
            original.encode(buf);
            HandshakePacket decoded = new HandshakePacket();
            assertThatThrownBy(() -> decoded.decode(buf)).isInstanceOf(ProtocolException.class);
        } finally {
            buf.release();
        }
    }

    @Test
    void handshake_bufferFullyConsumed() {
        HandshakePacket original = new HandshakePacket(1, "test:client");
        ByteBuf buf = Unpooled.buffer();
        try {
            original.encode(buf);
            new HandshakePacket().decode(buf);
            assertThat(buf.isReadable()).isFalse();
        } finally {
            buf.release();
        }
    }

    private HandshakePacket encodeAndDecode(HandshakePacket packet) {
        ByteBuf buf = Unpooled.buffer();
        try {
            packet.encode(buf);
            HandshakePacket decoded = new HandshakePacket();
            decoded.decode(buf);
            return decoded;
        } finally {
            buf.release();
        }
    }
}
