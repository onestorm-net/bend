package net.onestorm.bend.protocol.codec;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageDecoder;
import java.util.List;
import net.onestorm.bend.protocol.Protocol;
import net.onestorm.bend.protocol.exception.ProtocolException;
import net.onestorm.bend.protocol.packet.Packet;
import net.onestorm.bend.protocol.util.ProtocolUtil;

public class PacketDecoder extends MessageToMessageDecoder<ByteBuf> {

    private final Protocol protocol;
    private final Protocol.Direction direction;

    public PacketDecoder(Protocol protocol, Protocol.Direction direction) {
        this.protocol = protocol;
        this.direction = direction;
    }

    @Override
    protected void decode(ChannelHandlerContext context, ByteBuf buffer, List<Object> output) throws Exception {
        int packetId = ProtocolUtil.readVarInt(buffer);

        Packet packet = protocol.createPacket(packetId, direction);
        if (packet == null) {
            throw new ProtocolException("Unknown packet id " + packetId + " for direction " + direction);
        }

        packet.decode(buffer);

        if (buffer.isReadable()) {
            throw new ProtocolException("Packet " + packet.getClass().getSimpleName() + " (id=0x"
                    + Integer.toHexString(packetId) + ")" + " did not read all bytes; "
                    + buffer.readableBytes() + " byte(s) remaining");
        }

        output.add(packet);
    }
}
