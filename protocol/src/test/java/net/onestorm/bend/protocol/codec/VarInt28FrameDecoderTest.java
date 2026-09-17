package net.onestorm.bend.protocol.codec;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.embedded.EmbeddedChannel;
import net.onestorm.bend.protocol.exception.ProtocolException;
import net.onestorm.bend.protocol.util.ProtocolUtil;
import org.junit.jupiter.api.Test;

class VarInt28FrameDecoderTest {

    @Test
    void smallFrame_roundTrip() {
        EmbeddedChannel channel =
                new EmbeddedChannel(new VarInt28FrameDecoder(VarInt28FrameDecoder.MAXIMUM_FRAME_LENGTH));
        try {
            channel.writeInbound(frame(5));
            ByteBuf decoded = channel.readInbound();
            assertThat(decoded).isNotNull();
            decoded.release();
        } finally {
            channel.finishAndReleaseAll();
        }
    }

    @Test
    void frameRequiringFourByteLength_roundTrip() {
        // 2_200_000 needs 4 VarInt bytes (2_097_152 is the smallest value that does) — this exact
        // case threw "larger than 21 bits" before the fix, since the decoder only read 3 bytes.
        int length = 2_200_000;
        assertThat(ProtocolUtil.varIntLength(length)).isEqualTo(4);

        EmbeddedChannel channel =
                new EmbeddedChannel(new VarInt28FrameDecoder(VarInt28FrameDecoder.MAXIMUM_FRAME_LENGTH));
        try {
            channel.writeInbound(frame(length));
            ByteBuf decoded = channel.readInbound();
            assertThat(decoded).isNotNull();
            assertThat(decoded.readableBytes()).isEqualTo(length);
            decoded.release();
        } finally {
            channel.finishAndReleaseAll();
        }
    }

    @Test
    void fourByteLength_deliveredOneByteAtATime_stillDecodes() {
        // exercises readRawVarIntSmallBuf across multiple partial decode() calls: each of the first
        // 3 bytes must be treated as "wait for more", not as a premature truncate/throw, since a
        // valid 4-byte VarInt hasn't fully arrived yet.
        int length = 2_200_000;
        ByteBuf full = frame(length);

        EmbeddedChannel channel =
                new EmbeddedChannel(new VarInt28FrameDecoder(VarInt28FrameDecoder.MAXIMUM_FRAME_LENGTH));
        try {
            while (full.isReadable()) {
                channel.writeInbound(full.readRetainedSlice(1));
                if (full.isReadable()) {
                    assertThat((ByteBuf) channel.readInbound()).isNull();
                }
            }
            ByteBuf decoded = channel.readInbound();
            assertThat(decoded).isNotNull();
            assertThat(decoded.readableBytes()).isEqualTo(length);
            decoded.release();
        } finally {
            full.release();
            channel.finishAndReleaseAll();
        }
    }

    @Test
    void lengthAtConfiguredMaximum_accepted() {
        EmbeddedChannel channel = new EmbeddedChannel(new VarInt28FrameDecoder(10));
        try {
            channel.writeInbound(frame(10));
            ByteBuf decoded = channel.readInbound();
            assertThat(decoded).isNotNull();
            decoded.release();
        } finally {
            channel.finishAndReleaseAll();
        }
    }

    @Test
    void lengthOverConfiguredMaximum_throws() {
        EmbeddedChannel channel = new EmbeddedChannel(new VarInt28FrameDecoder(10));
        try {
            assertThatThrownBy(() -> channel.writeInbound(frame(11))).hasCauseInstanceOf(ProtocolException.class);
        } finally {
            channel.finishAndReleaseAll();
        }
    }

    @Test
    void configuredMaximum_aboveWireCeiling_isClampedDown() {
        VarInt28FrameDecoder decoder = new VarInt28FrameDecoder(Integer.MAX_VALUE);
        assertThat(decoder.getMaximumFrameLength()).isEqualTo(VarInt28FrameDecoder.MAXIMUM_FRAME_LENGTH);
    }

    @Test
    void varIntWiderThan28Bits_fastPath_throws() {
        // 5 bytes, all with the continuation bit set: readableBytes() >= 4 so this hits the fast
        // path, which must reject it rather than silently reading past the supported width.
        ByteBuf malformed = Unpooled.buffer()
                .writeBytes(new byte[] {(byte) 0x80, (byte) 0x80, (byte) 0x80, (byte) 0x80, (byte) 0x80});

        EmbeddedChannel channel =
                new EmbeddedChannel(new VarInt28FrameDecoder(VarInt28FrameDecoder.MAXIMUM_FRAME_LENGTH));
        try {
            assertThatThrownBy(() -> channel.writeInbound(malformed))
                    .hasCauseInstanceOf(ProtocolException.class)
                    .cause()
                    .hasMessageContaining("28 bits");
        } finally {
            channel.finishAndReleaseAll();
        }
    }

    @Test
    void leadingNulPadding_belowThreshold_waitsForMore() {
        // FIND_NON_NUL never finds a non-zero byte; with <=16 readable, that just means "not enough
        // data yet", not a protocol violation.
        EmbeddedChannel channel =
                new EmbeddedChannel(new VarInt28FrameDecoder(VarInt28FrameDecoder.MAXIMUM_FRAME_LENGTH));
        try {
            channel.writeInbound(Unpooled.buffer().writeBytes(new byte[10]));
            assertThat((ByteBuf) channel.readInbound()).isNull();
        } finally {
            channel.finishAndReleaseAll();
        }
    }

    @Test
    void leadingNulPadding_aboveThreshold_throws() {
        EmbeddedChannel channel =
                new EmbeddedChannel(new VarInt28FrameDecoder(VarInt28FrameDecoder.MAXIMUM_FRAME_LENGTH));
        try {
            ByteBuf allNul = Unpooled.buffer().writeBytes(new byte[17]);
            assertThatThrownBy(() -> channel.writeInbound(allNul)).hasCauseInstanceOf(ProtocolException.class);
        } finally {
            channel.finishAndReleaseAll();
        }
    }

    @Test
    void nonCanonicalTwoByteZeroLength_smallBufferPath_throws() {
        // 0x80 0x00 is a non-minimal (but valid) 2-byte encoding of 0: readableBytes() < 4 so this
        // is handled by readRawVarIntSmallBuf. The leading byte (0x80) is non-NUL, so FIND_NON_NUL's
        // padding skip never engages — a decoded length of 0 must still be rejected here, since a
        // frame always carries at least a 1-byte packet id.
        EmbeddedChannel channel =
                new EmbeddedChannel(new VarInt28FrameDecoder(VarInt28FrameDecoder.MAXIMUM_FRAME_LENGTH));
        try {
            assertThatThrownBy(() -> channel.writeInbound(Unpooled.buffer().writeBytes(new byte[] {(byte) 0x80, 0x00})))
                    .hasCauseInstanceOf(ProtocolException.class)
                    .cause()
                    .hasMessageContaining("zero");
        } finally {
            channel.finishAndReleaseAll();
        }
    }

    @Test
    void nonCanonicalThreeByteZeroLength_smallBufferPath_throws() {
        EmbeddedChannel channel =
                new EmbeddedChannel(new VarInt28FrameDecoder(VarInt28FrameDecoder.MAXIMUM_FRAME_LENGTH));
        try {
            assertThatThrownBy(() -> channel.writeInbound(
                            Unpooled.buffer().writeBytes(new byte[] {(byte) 0x80, (byte) 0x80, 0x00})))
                    .hasCauseInstanceOf(ProtocolException.class)
                    .cause()
                    .hasMessageContaining("zero");
        } finally {
            channel.finishAndReleaseAll();
        }
    }

    @Test
    void fourByteZeroLength_fastPath_throws() {
        // exercises the length==0 check via the fast path (readableBytes() >= 4), not just the
        // small-buffer path covered by the two tests above
        EmbeddedChannel channel =
                new EmbeddedChannel(new VarInt28FrameDecoder(VarInt28FrameDecoder.MAXIMUM_FRAME_LENGTH));
        try {
            ByteBuf malformed = Unpooled.buffer().writeBytes(new byte[] {(byte) 0x80, (byte) 0x80, (byte) 0x80, 0x00});
            assertThatThrownBy(() -> channel.writeInbound(malformed))
                    .hasCauseInstanceOf(ProtocolException.class)
                    .cause()
                    .hasMessageContaining("zero");
        } finally {
            channel.finishAndReleaseAll();
        }
    }

    @Test
    void totalBufferUnderFourBytes_singleByteLength_usesSmallBufferPath() {
        // total readable bytes (2) is under the fast-path's 4-byte threshold even though the length
        // VarInt itself is a single terminal byte — exercises readRawVarIntSmallBuf's 1-byte return.
        EmbeddedChannel channel =
                new EmbeddedChannel(new VarInt28FrameDecoder(VarInt28FrameDecoder.MAXIMUM_FRAME_LENGTH));
        try {
            channel.writeInbound(frame(1));
            ByteBuf decoded = channel.readInbound();
            assertThat(decoded).isNotNull();
            assertThat(decoded.readableBytes()).isEqualTo(1);
            decoded.release();
        } finally {
            channel.finishAndReleaseAll();
        }
    }

    private static ByteBuf frame(int contentLength) {
        ByteBuf buf = Unpooled.buffer();
        ProtocolUtil.writeVarInt(buf, contentLength);
        buf.writeBytes(new byte[contentLength]);
        return buf;
    }
}
