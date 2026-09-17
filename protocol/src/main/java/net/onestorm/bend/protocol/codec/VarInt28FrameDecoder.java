package net.onestorm.bend.protocol.codec;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import io.netty.util.ByteProcessor;
import java.util.List;
import lombok.Getter;
import net.onestorm.bend.protocol.exception.ProtocolException;

public class VarInt28FrameDecoder extends ByteToMessageDecoder {

    // the largest length a 28-bit VarInt can represent
    public static final int MAXIMUM_FRAME_LENGTH = (1 << 28) - 1;

    @Getter
    private final int maximumFrameLength;

    public VarInt28FrameDecoder(int maximumFrameLength) {
        // never allow a configured cap above what the VarInt reader can actually represent
        this.maximumFrameLength = Math.min(maximumFrameLength, MAXIMUM_FRAME_LENGTH);
    }

    @Override
    protected void decode(ChannelHandlerContext context, ByteBuf buffer, List<Object> output) throws Exception {

        if (!context.channel().isActive()) {
            buffer.clear();
            return;
        }

        int readableBytes = buffer.readableBytes();
        int contentStart = buffer.forEachByte(ByteProcessor.FIND_NON_NUL);

        if (contentStart == -1) {
            buffer.clear();
            if (readableBytes > 16) {
                throw new ProtocolException("No content start could be found in more than 16 readable bytes");
            }
            return;
        }

        // set reader at the start
        buffer.readerIndex(contentStart);
        // and mark it, so we can revisit it
        buffer.markReaderIndex();

        try {
            // try reading var int
            int length = readRawVarInt(buffer);

            if (contentStart == buffer.readerIndex()) {
                return; // readerIndex didn't move
            }

            if (length > maximumFrameLength) {
                throw new ProtocolException(
                        "Content length " + length + " exceeds maximum frame length " + maximumFrameLength);
            }

            // a frame always carries at least a 1-byte packet id, so a length of 0 is never valid —
            // this can still occur via a non-minimal multibyte VarInt encoding (e.g. 0x80 0x00) that
            // starts with a non-NUL byte, so it isn't caught by the leading-NUL-padding skip above
            if (length == 0) {
                throw new ProtocolException("Content length is zero; a frame must contain at least a packet id byte");
            }

            if (buffer.readableBytes() < length) {
                buffer.resetReaderIndex(); // we don't have all content yet
            } else {
                output.add(buffer.readRetainedSlice(length));
            }

        } catch (Exception e) {
            buffer.resetReaderIndex(); // fixes a memory leak
            throw e;
        }
    }

    private static int readRawVarInt(ByteBuf buffer) {
        if (buffer.readableBytes() < 4) {
            return readRawVarIntSmallBuf(buffer);
        }
        int wholeOrMore = buffer.getIntLE(buffer.readerIndex());

        // take all four bytes and check if any of them have the high bit set
        int atStop = ~wholeOrMore & 0x80808080;
        if (atStop == 0) {
            // all bytes have the high bit set, so the VarInt we are trying to decode is too wide
            throw new ProtocolException("Provided VarInt is larger than 28 bits");
        }

        int bitsToKeep = Integer.numberOfTrailingZeros(atStop) + 1;
        buffer.skipBytes(bitsToKeep >> 3);

        // remove all bits we don't need to keep, a trick from
        // https://github.com/netty/netty/pull/14050#issuecomment-2107750734:
        //
        // > The idea is that thisVarIntMask has 0s above the first one of firstOneOnStop, and 1s at
        // > and below it. For example if firstOneOnStop is 0x800080 (where the last 0x80 is the only
        // > one that matters), then thisVarIntMask is 0xFF.
        //
        // this is also documented in Hacker's Delight, section 2-1 "Manipulating Rightmost Bits"
        int preservedBytes = wholeOrMore & (atStop ^ (atStop - 1));

        // merge together using this trick: https://github.com/netty/netty/pull/14050#discussion_r1597896639
        preservedBytes = (preservedBytes & 0x007F007F) | ((preservedBytes & 0x7F007F00) >> 1);
        preservedBytes = (preservedBytes & 0x00003FFF) | ((preservedBytes & 0x3FFF0000) >> 2);
        return preservedBytes;
    }

    private static int readRawVarIntSmallBuf(ByteBuf buffer) {
        // always called with readableBytes() in [1, 3] — decode() only reaches here after
        // FIND_NON_NUL has found a byte, and Netty never calls decode() with an empty buffer
        buffer.markReaderIndex();

        byte tmp = buffer.readByte();
        if (tmp >= 0) {
            return tmp;
        }
        int result = tmp & 0x7F;
        if (!buffer.isReadable()) {
            buffer.resetReaderIndex();
            return 0;
        }
        if ((tmp = buffer.readByte()) >= 0) {
            return result | tmp << 7;
        }
        result |= (tmp & 0x7F) << 7;
        if (!buffer.isReadable()) {
            buffer.resetReaderIndex();
            return 0;
        }
        if ((tmp = buffer.readByte()) >= 0) {
            return result | tmp << 14;
        }

        // 4 byte should not be possible, better to just wait for it and let getIntLE handle it
        buffer.resetReaderIndex();
        return 0;
    }
}
