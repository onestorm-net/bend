package net.onestorm.bend.protocol.codec;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.embedded.EmbeddedChannel;
import net.onestorm.bend.protocol.exception.ProtocolException;
import net.onestorm.bend.protocol.util.ProtocolUtil;
import org.junit.jupiter.api.Test;

class VarInt28LengthEncoderTest {

    @Test
    void encode_prefixesPayloadWithVarIntLength() {
        EmbeddedChannel channel =
                new EmbeddedChannel(new VarInt28LengthEncoder(VarInt28LengthEncoder.MAXIMUM_FRAME_LENGTH));
        try {
            byte[] payload = "hello".getBytes();
            channel.writeOutbound(Unpooled.wrappedBuffer(payload));

            ByteBuf lengthPrefix = channel.readOutbound();
            ByteBuf body = channel.readOutbound();
            try {
                assertThat(ProtocolUtil.readVarInt(lengthPrefix)).isEqualTo(payload.length);
                assertThat(body.readableBytes()).isEqualTo(payload.length);
            } finally {
                lengthPrefix.release();
                body.release();
            }
        } finally {
            channel.finishAndReleaseAll();
        }
    }

    @Test
    void encode_lengthOverConfiguredMaximum_throws() {
        EmbeddedChannel channel = new EmbeddedChannel(new VarInt28LengthEncoder(10));
        try {
            ByteBuf oversized = Unpooled.buffer().writeBytes(new byte[11]);
            assertThatThrownBy(() -> channel.writeOutbound(oversized)).hasCauseInstanceOf(ProtocolException.class);
        } finally {
            channel.finishAndReleaseAll();
        }
    }

    @Test
    void encode_lengthAtConfiguredMaximum_accepted() {
        EmbeddedChannel channel = new EmbeddedChannel(new VarInt28LengthEncoder(10));
        try {
            channel.writeOutbound(Unpooled.buffer().writeBytes(new byte[10]));
            assertThat((ByteBuf) channel.readOutbound()).isNotNull();
        } finally {
            channel.finishAndReleaseAll();
        }
    }

    @Test
    void configuredMaximum_aboveWireCeiling_isClampedDown() {
        VarInt28LengthEncoder encoder = new VarInt28LengthEncoder(Integer.MAX_VALUE);
        assertThat(encoder.getMaximumFrameLength()).isEqualTo(VarInt28LengthEncoder.MAXIMUM_FRAME_LENGTH);
    }
}
