package net.onestorm.bend.protocol.packet;

import static org.assertj.core.api.Assertions.assertThat;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.jupiter.api.Test;

class SupportedEndpointsPacketTest {

    @Test
    void supportedEndpoints_roundTrip() {
        SupportedEndpointsPacket original = new SupportedEndpointsPacket(3, 1, new int[] {1, 2, 3});
        SupportedEndpointsPacket decoded = encodeAndDecode(original);

        assertThat(decoded.getAvailableRevision()).isEqualTo(3);
        assertThat(decoded.getSupportedRevision()).isEqualTo(1);
        assertThat(decoded.getEndpointIds()).containsExactly(1, 2, 3);
    }

    @Test
    void supportedEndpoints_emptyIds_roundTrip() {
        SupportedEndpointsPacket original = new SupportedEndpointsPacket(1, 1, new int[0]);
        SupportedEndpointsPacket decoded = encodeAndDecode(original);

        assertThat(decoded.getEndpointIds()).isEmpty();
    }

    @Test
    void supportedEndpoints_bufferFullyConsumed() {
        SupportedEndpointsPacket original = new SupportedEndpointsPacket(1, 1, new int[] {1, 2});
        ByteBuf buf = Unpooled.buffer();
        try {
            original.encode(buf);
            new SupportedEndpointsPacket().decode(buf);
            assertThat(buf.isReadable()).isFalse();
        } finally {
            buf.release();
        }
    }

    private SupportedEndpointsPacket encodeAndDecode(SupportedEndpointsPacket packet) {
        ByteBuf buf = Unpooled.buffer();
        try {
            packet.encode(buf);
            SupportedEndpointsPacket decoded = new SupportedEndpointsPacket();
            decoded.decode(buf);
            return decoded;
        } finally {
            buf.release();
        }
    }
}
