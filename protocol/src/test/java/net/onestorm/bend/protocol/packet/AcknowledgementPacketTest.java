package net.onestorm.bend.protocol.packet;

import static org.assertj.core.api.Assertions.assertThat;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class AcknowledgementPacketTest {

    @ParameterizedTest
    @ValueSource(ints = {0, 1, 128, Integer.MAX_VALUE})
    void availableEndpointsAcknowledged_roundTrip(int revision) {
        ByteBuf buf = Unpooled.buffer();
        try {
            new AvailableEndpointsAcknowledgedPacket(revision).encode(buf);
            AvailableEndpointsAcknowledgedPacket decoded = new AvailableEndpointsAcknowledgedPacket();
            decoded.decode(buf);

            assertThat(decoded.getAvailableRevision()).isEqualTo(revision);
            assertThat(buf.isReadable()).isFalse();
        } finally {
            buf.release();
        }
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1, 128, Integer.MAX_VALUE})
    void supportedEndpointsAcknowledged_roundTrip(int revision) {
        ByteBuf buf = Unpooled.buffer();
        try {
            new SupportedEndpointsAcknowledgedPacket(revision).encode(buf);
            SupportedEndpointsAcknowledgedPacket decoded = new SupportedEndpointsAcknowledgedPacket();
            decoded.decode(buf);

            assertThat(decoded.getSupportedRevision()).isEqualTo(revision);
            assertThat(buf.isReadable()).isFalse();
        } finally {
            buf.release();
        }
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1, 128, Integer.MAX_VALUE})
    void keepAlive_roundTrip(int value) {
        ByteBuf buf = Unpooled.buffer();
        try {
            new KeepAlivePacket(value).encode(buf);
            KeepAlivePacket decoded = new KeepAlivePacket();
            decoded.decode(buf);

            assertThat(decoded.getValue()).isEqualTo(value);
            assertThat(buf.isReadable()).isFalse();
        } finally {
            buf.release();
        }
    }
}
