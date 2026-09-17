package net.onestorm.bend.protocol.packet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.onestorm.bend.protocol.endpoint.Endpoint;
import net.onestorm.bend.protocol.exception.ProtocolException;
import net.onestorm.bend.protocol.util.ProtocolUtil;
import org.junit.jupiter.api.Test;

class AvailableEndpointsPacketTest {

    @Test
    void availableEndpoints_singleEndpoint_roundTrip() {
        Endpoint endpoint = Endpoint.of(1, "test:endpoint", true, false);
        AvailableEndpointsPacket original = new AvailableEndpointsPacket(5, new Endpoint[] {endpoint});
        AvailableEndpointsPacket decoded = encodeAndDecode(original);

        assertThat(decoded.getAvailableRevision()).isEqualTo(5);
        assertThat(decoded.getEndpoints()).hasSize(1);
        assertEndpoint(decoded.getEndpoints()[0], 1, "test:endpoint", 0x01);
    }

    @Test
    void availableEndpoints_multipleEndpoints_roundTrip() {
        Endpoint[] endpoints = {
            Endpoint.of(1, "ns:clientbound", true, false),
            Endpoint.of(2, "ns:serverbound", false, true),
            Endpoint.of(3, "ns:bidirectional", true, true),
        };
        AvailableEndpointsPacket original = new AvailableEndpointsPacket(1, endpoints);
        AvailableEndpointsPacket decoded = encodeAndDecode(original);

        assertThat(decoded.getEndpoints()).hasSize(3);
        assertEndpoint(decoded.getEndpoints()[0], 1, "ns:clientbound", 0x01);
        assertEndpoint(decoded.getEndpoints()[1], 2, "ns:serverbound", 0x02);
        assertEndpoint(decoded.getEndpoints()[2], 3, "ns:bidirectional", 0x03);
    }

    @Test
    void availableEndpoints_endpointCountExceedsAvailableBytes_throwsWithoutAllocating() {
        // claims a billion endpoints but provides none of their fields — must be rejected before
        // `new Endpoint[length]` is even attempted, since each endpoint needs at least 1 readable byte
        ByteBuf buf = Unpooled.buffer();
        try {
            ProtocolUtil.writeVarInt(buf, 1); // availableRevision
            ProtocolUtil.writeVarInt(buf, 1_000_000_000); // claimed endpoint count
            AvailableEndpointsPacket decoded = new AvailableEndpointsPacket();
            assertThatThrownBy(() -> decoded.decode(buf)).isInstanceOf(ProtocolException.class);
        } finally {
            buf.release();
        }
    }

    @Test
    void availableEndpoints_emptyEndpoints_roundTrip() {
        AvailableEndpointsPacket original = new AvailableEndpointsPacket(1, new Endpoint[0]);
        AvailableEndpointsPacket decoded = encodeAndDecode(original);

        assertThat(decoded.getEndpoints()).isEmpty();
    }

    @Test
    void availableEndpoints_bufferFullyConsumed() {
        Endpoint endpoint = Endpoint.of(1, "test:endpoint", true, false);
        AvailableEndpointsPacket original = new AvailableEndpointsPacket(1, new Endpoint[] {endpoint});
        ByteBuf buf = Unpooled.buffer();
        try {
            original.encode(buf);
            new AvailableEndpointsPacket().decode(buf);
            assertThat(buf.isReadable()).isFalse();
        } finally {
            buf.release();
        }
    }

    private AvailableEndpointsPacket encodeAndDecode(AvailableEndpointsPacket packet) {
        ByteBuf buf = Unpooled.buffer();
        try {
            packet.encode(buf);
            AvailableEndpointsPacket decoded = new AvailableEndpointsPacket();
            decoded.decode(buf);
            return decoded;
        } finally {
            buf.release();
        }
    }

    private void assertEndpoint(Endpoint endpoint, int id, String name, int flags) {
        assertThat(endpoint.getId()).isEqualTo(id);
        assertThat(endpoint.getName()).isEqualTo(name);
        assertThat(endpoint.getFlags()).isEqualTo(flags);
    }
}
