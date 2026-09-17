package net.onestorm.bend.protocol.codec;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.embedded.EmbeddedChannel;
import net.onestorm.bend.protocol.Protocol;
import net.onestorm.bend.protocol.ProtocolV1;
import net.onestorm.bend.protocol.exception.ProtocolException;
import net.onestorm.bend.protocol.packet.HandshakePacket;
import net.onestorm.bend.protocol.util.ProtocolUtil;
import org.junit.jupiter.api.Test;

class PacketDecoderTest {

    private final Protocol protocol = new ProtocolV1();

    @Test
    void decode_knownPacketId_producesDecodedPacket() {
        EmbeddedChannel channel = new EmbeddedChannel(new PacketDecoder(protocol, Protocol.Direction.TO_SERVER));
        try {
            ByteBuf buf = Unpooled.buffer();
            ProtocolUtil.writeVarInt(buf, 0x01); // HandshakePacket, to-server
            new HandshakePacket(1, "test:client").encode(buf);

            channel.writeInbound(buf);
            HandshakePacket decoded = channel.readInbound();

            assertThat(decoded).isNotNull();
            assertThat(decoded.getProtocolVersionNumber()).isEqualTo(1);
            assertThat(decoded.getClientIdentifier()).isEqualTo("test:client");
        } finally {
            channel.finishAndReleaseAll();
        }
    }

    @Test
    void decode_unknownPacketId_throws() {
        EmbeddedChannel channel = new EmbeddedChannel(new PacketDecoder(protocol, Protocol.Direction.TO_SERVER));
        try {
            ByteBuf buf = Unpooled.buffer();
            ProtocolUtil.writeVarInt(buf, 0xFE); // never registered

            assertThatThrownBy(() -> channel.writeInbound(buf)).hasCauseInstanceOf(ProtocolException.class);
        } finally {
            channel.finishAndReleaseAll();
        }
    }

    @Test
    void decode_leftoverBytesAfterPacketDecode_throws() {
        EmbeddedChannel channel = new EmbeddedChannel(new PacketDecoder(protocol, Protocol.Direction.TO_SERVER));
        try {
            ByteBuf buf = Unpooled.buffer();
            ProtocolUtil.writeVarInt(buf, 0x01); // HandshakePacket
            new HandshakePacket(1, "test:client").encode(buf);
            buf.writeByte(0x00); // trailing garbage the packet didn't consume

            assertThatThrownBy(() -> channel.writeInbound(buf)).hasCauseInstanceOf(ProtocolException.class);
        } finally {
            channel.finishAndReleaseAll();
        }
    }
}
