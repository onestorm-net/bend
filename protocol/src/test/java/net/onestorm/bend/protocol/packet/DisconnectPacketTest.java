package net.onestorm.bend.protocol.packet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.onestorm.bend.protocol.exception.ProtocolException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class DisconnectPacketTest {

    @ParameterizedTest
    @ValueSource(ints = {0, 1, 2, 3})
    void disconnect_allTypes_roundTrip(int type) {
        DisconnectPacket original = new DisconnectPacket(type, "reason");
        DisconnectPacket decoded = encodeAndDecode(original);

        assertThat(decoded.getType()).isEqualTo(type);
        assertThat(decoded.getReason()).isEqualTo("reason");
    }

    @Test
    void disconnect_emptyReason_roundTrip() {
        DisconnectPacket original = new DisconnectPacket(0, "");
        DisconnectPacket decoded = encodeAndDecode(original);

        assertThat(decoded.getReason()).isEqualTo("");
    }

    @Test
    void disconnect_maxLengthReason_accepted() {
        String reason = "a".repeat(256);
        DisconnectPacket original = new DisconnectPacket(0, reason);
        DisconnectPacket decoded = encodeAndDecode(original);

        assertThat(decoded.getReason()).isEqualTo(reason);
    }

    @Test
    void disconnect_overMaxLengthReason_throws() {
        DisconnectPacket original = new DisconnectPacket(0, "a".repeat(257));
        ByteBuf buf = Unpooled.buffer();
        try {
            original.encode(buf);
            DisconnectPacket decoded = new DisconnectPacket();
            assertThatThrownBy(() -> decoded.decode(buf)).isInstanceOf(ProtocolException.class);
        } finally {
            buf.release();
        }
    }

    @Test
    void disconnect_bufferFullyConsumed() {
        DisconnectPacket original = new DisconnectPacket(1, "protocol error");
        ByteBuf buf = Unpooled.buffer();
        try {
            original.encode(buf);
            new DisconnectPacket().decode(buf);
            assertThat(buf.isReadable()).isFalse();
        } finally {
            buf.release();
        }
    }

    private DisconnectPacket encodeAndDecode(DisconnectPacket packet) {
        ByteBuf buf = Unpooled.buffer();
        try {
            packet.encode(buf);
            DisconnectPacket decoded = new DisconnectPacket();
            decoded.decode(buf);
            return decoded;
        } finally {
            buf.release();
        }
    }
}
