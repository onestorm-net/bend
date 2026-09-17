package net.onestorm.bend.protocol;

import net.onestorm.bend.protocol.packet.AvailableEndpointsAcknowledgedPacket;
import net.onestorm.bend.protocol.packet.AvailableEndpointsPacket;
import net.onestorm.bend.protocol.packet.DisconnectPacket;
import net.onestorm.bend.protocol.packet.EndpointMessagePacket;
import net.onestorm.bend.protocol.packet.HandshakePacket;
import net.onestorm.bend.protocol.packet.KeepAlivePacket;
import net.onestorm.bend.protocol.packet.SupportedEndpointsAcknowledgedPacket;
import net.onestorm.bend.protocol.packet.SupportedEndpointsPacket;
import org.jspecify.annotations.NullMarked;

@NullMarked
public class ProtocolV1 extends AbstractProtocol {

    private static final int VERSION_NUMBER = 1;

    public ProtocolV1() {
        toClient().registerPacket(0x01, DisconnectPacket.class, DisconnectPacket::new);
        toClient().registerPacket(0x02, AvailableEndpointsPacket.class, AvailableEndpointsPacket::new);
        toClient()
                .registerPacket(
                        0x03, SupportedEndpointsAcknowledgedPacket.class, SupportedEndpointsAcknowledgedPacket::new);
        toClient().registerPacket(0x04, EndpointMessagePacket.class, EndpointMessagePacket::new);
        toClient().registerPacket(0x05, KeepAlivePacket.class, KeepAlivePacket::new);

        toServer().registerPacket(0x01, HandshakePacket.class, HandshakePacket::new);
        toServer()
                .registerPacket(
                        0x02, AvailableEndpointsAcknowledgedPacket.class, AvailableEndpointsAcknowledgedPacket::new);
        toServer().registerPacket(0x03, SupportedEndpointsPacket.class, SupportedEndpointsPacket::new);
        toServer().registerPacket(0x04, EndpointMessagePacket.class, EndpointMessagePacket::new);
        toServer().registerPacket(0x05, KeepAlivePacket.class, KeepAlivePacket::new);
    }

    @Override
    public int getVersionNumber() {
        return VERSION_NUMBER;
    }
}
