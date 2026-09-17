package net.onestorm.bend.protocol;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.stream.Stream;
import net.onestorm.bend.protocol.packet.AvailableEndpointsAcknowledgedPacket;
import net.onestorm.bend.protocol.packet.AvailableEndpointsPacket;
import net.onestorm.bend.protocol.packet.DisconnectPacket;
import net.onestorm.bend.protocol.packet.EndpointMessagePacket;
import net.onestorm.bend.protocol.packet.HandshakePacket;
import net.onestorm.bend.protocol.packet.KeepAlivePacket;
import net.onestorm.bend.protocol.packet.Packet;
import net.onestorm.bend.protocol.packet.SupportedEndpointsAcknowledgedPacket;
import net.onestorm.bend.protocol.packet.SupportedEndpointsPacket;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;

class ProtocolV1Test {

    private final ProtocolV1 protocol = new ProtocolV1();

    static Stream<Arguments> registeredPackets() {
        return Stream.of(
                Arguments.of(Protocol.Direction.TO_CLIENT, 0x01, DisconnectPacket.class),
                Arguments.of(Protocol.Direction.TO_CLIENT, 0x02, AvailableEndpointsPacket.class),
                Arguments.of(Protocol.Direction.TO_CLIENT, 0x03, SupportedEndpointsAcknowledgedPacket.class),
                Arguments.of(Protocol.Direction.TO_CLIENT, 0x04, EndpointMessagePacket.class),
                Arguments.of(Protocol.Direction.TO_CLIENT, 0x05, KeepAlivePacket.class),
                Arguments.of(Protocol.Direction.TO_SERVER, 0x01, HandshakePacket.class),
                Arguments.of(Protocol.Direction.TO_SERVER, 0x02, AvailableEndpointsAcknowledgedPacket.class),
                Arguments.of(Protocol.Direction.TO_SERVER, 0x03, SupportedEndpointsPacket.class),
                Arguments.of(Protocol.Direction.TO_SERVER, 0x04, EndpointMessagePacket.class),
                Arguments.of(Protocol.Direction.TO_SERVER, 0x05, KeepAlivePacket.class));
    }

    @ParameterizedTest
    @MethodSource("registeredPackets")
    void getPacketId_matchesRegisteredId(Protocol.Direction direction, int id, Class<? extends Packet> packetClass) {
        assertThat(protocol.getPacketId(packetClass, direction)).isEqualTo(id);
    }

    @ParameterizedTest
    @MethodSource("registeredPackets")
    void createPacket_returnsInstanceOfRegisteredClass(
            Protocol.Direction direction, int id, Class<? extends Packet> packetClass) {
        assertThat(protocol.createPacket(id, direction)).isInstanceOf(packetClass);
    }

    @ParameterizedTest
    @EnumSource(Protocol.Direction.class)
    void createPacket_unregisteredId_returnsNull(Protocol.Direction direction) {
        assertThat(protocol.createPacket(0x06, direction)).isNull();
    }

    @Test
    void getVersionNumber_isOne() {
        assertThat(protocol.getVersionNumber()).isEqualTo(1);
    }
}
