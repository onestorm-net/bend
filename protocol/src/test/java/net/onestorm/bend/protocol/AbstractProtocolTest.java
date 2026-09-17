package net.onestorm.bend.protocol;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import net.onestorm.bend.protocol.packet.DisconnectPacket;
import net.onestorm.bend.protocol.packet.HandshakePacket;
import org.junit.jupiter.api.Test;

class AbstractProtocolTest {

    private static final class TestProtocol extends AbstractProtocol {
        TestProtocol() {
            toServer().registerPacket(0x01, HandshakePacket.class, HandshakePacket::new);
            toClient().registerPacket(0x01, DisconnectPacket.class, DisconnectPacket::new);
        }

        @Override
        public int getVersionNumber() {
            return 0;
        }
    }

    private final TestProtocol protocol = new TestProtocol();

    @Test
    void getPacketId_registered_returnsRegisteredId() {
        assertThat(protocol.getPacketId(HandshakePacket.class, Protocol.Direction.TO_SERVER))
                .isEqualTo(0x01);
    }

    @Test
    void getPacketId_unregisteredClass_throws() {
        assertThatThrownBy(() -> protocol.getPacketId(DisconnectPacket.class, Protocol.Direction.TO_SERVER))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void createPacket_registeredId_returnsNewInstanceOfRegisteredClass() {
        assertThat(protocol.createPacket(0x01, Protocol.Direction.TO_SERVER)).isInstanceOf(HandshakePacket.class);
    }

    @Test
    void createPacket_unregisteredIdWithinRange_returnsNull() {
        assertThat(protocol.createPacket(0x02, Protocol.Direction.TO_SERVER)).isNull();
    }

    @Test
    void createPacket_negativeId_returnsNull() {
        assertThat(protocol.createPacket(-1, Protocol.Direction.TO_SERVER)).isNull();
    }

    @Test
    void createPacket_idAboveMaximum_returnsNull() {
        assertThat(protocol.createPacket(0x100, Protocol.Direction.TO_SERVER)).isNull();
    }

    @Test
    void createPacket_idAtMaximum_doesNotThrow() {
        assertThat(protocol.createPacket(0xFF, Protocol.Direction.TO_SERVER)).isNull();
    }

    @Test
    void registerPacket_negativeId_throws() {
        assertThatThrownBy(() -> new AbstractProtocol() {
                    {
                        toServer().registerPacket(-1, HandshakePacket.class, HandshakePacket::new);
                    }

                    @Override
                    public int getVersionNumber() {
                        return 0;
                    }
                })
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void registerPacket_idAboveMaximum_throws() {
        assertThatThrownBy(() -> new AbstractProtocol() {
                    {
                        toServer().registerPacket(0x100, HandshakePacket.class, HandshakePacket::new);
                    }

                    @Override
                    public int getVersionNumber() {
                        return 0;
                    }
                })
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void directions_areIndependent() {
        // registered to server, not to client, at the same id
        assertThat(protocol.createPacket(0x01, Protocol.Direction.TO_SERVER)).isInstanceOf(HandshakePacket.class);
        assertThat(protocol.createPacket(0x01, Protocol.Direction.TO_CLIENT)).isInstanceOf(DisconnectPacket.class);
    }
}
