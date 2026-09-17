package net.onestorm.bend.protocol.codec;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageEncoder;
import java.util.List;
import lombok.Getter;
import net.onestorm.bend.protocol.exception.ProtocolException;
import net.onestorm.bend.protocol.util.ProtocolUtil;

@ChannelHandler.Sharable
public class VarInt28LengthEncoder extends MessageToMessageEncoder<ByteBuf> {

    // the largest length a 28-bit VarInt can represent
    public static final int MAXIMUM_FRAME_LENGTH = (1 << 28) - 1;

    @Getter
    private final int maximumFrameLength;

    public VarInt28LengthEncoder(int maximumFrameLength) {
        // never allow a configured cap above what the paired decoder's VarInt reader can represent
        this.maximumFrameLength = Math.min(maximumFrameLength, MAXIMUM_FRAME_LENGTH);
    }

    @Override
    protected void encode(ChannelHandlerContext context, ByteBuf buffer, List<Object> output) throws Exception {
        final int length = buffer.readableBytes();

        if (length > maximumFrameLength) {
            throw new ProtocolException(
                    "Content length " + length + " exceeds maximum frame length " + maximumFrameLength);
        }

        // how many bytes are needed to store the length in a variable integer?
        final int lengthFieldLength = ProtocolUtil.varIntLength(length);

        // there are cases were heap buffer could be more performant, will look into this later
        ByteBuf lengthFieldBuffer = context.alloc().directBuffer(lengthFieldLength);

        // fill the buffer with the length as a variable integer
        ProtocolUtil.writeVarInt(lengthFieldBuffer, length);

        output.add(lengthFieldBuffer);
        output.add(buffer.retain());
    }
}
