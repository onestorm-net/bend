package net.onestorm.bend.protocol.codec;

import static org.assertj.core.api.Assertions.assertThat;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.embedded.EmbeddedChannel;
import net.onestorm.bend.protocol.Protocol;
import net.onestorm.bend.protocol.ProtocolV1;
import net.onestorm.bend.protocol.packet.HandshakePacket;
import net.onestorm.bend.protocol.util.ProtocolUtil;
import org.junit.jupiter.api.Test;

class PacketEncoderTest {

    private final Protocol protocol = new ProtocolV1();

    @Test
    void encode_writesPacketIdFollowedByPacketPayload() {
        EmbeddedChannel channel = new EmbeddedChannel(new PacketEncoder(protocol, Protocol.Direction.TO_SERVER));
        try {
            HandshakePacket packet = new HandshakePacket(1, "test:client");
            channel.writeOutbound(packet);

            ByteBuf output = channel.readOutbound();
            try {
                assertThat(ProtocolUtil.readVarInt(output)).isEqualTo(0x01);

                ByteBuf expectedPayload = Unpooled.buffer();
                try {
                    packet.encode(expectedPayload);
                    assertThat(output.readableBytes()).isEqualTo(expectedPayload.readableBytes());
                    assertThat(output.readSlice(output.readableBytes())).isEqualTo(expectedPayload);
                } finally {
                    expectedPayload.release();
                }
            } finally {
                output.release();
            }
        } finally {
            channel.finishAndReleaseAll();
        }
    }
}
