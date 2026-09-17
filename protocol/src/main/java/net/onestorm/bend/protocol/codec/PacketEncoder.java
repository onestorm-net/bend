package net.onestorm.bend.protocol.codec;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import net.onestorm.bend.protocol.Protocol;
import net.onestorm.bend.protocol.packet.Packet;
import net.onestorm.bend.protocol.util.ProtocolUtil;

public class PacketEncoder extends MessageToByteEncoder<Packet> {

    private final Protocol protocol;
    private final Protocol.Direction direction;

    public PacketEncoder(Protocol protocol, Protocol.Direction direction) {
        this.protocol = protocol;
        this.direction = direction;
    }

    @Override
    protected void encode(ChannelHandlerContext context, Packet packet, ByteBuf output) throws Exception {
        int packetId = protocol.getPacketId(packet.getClass(), direction);
        ProtocolUtil.writeVarInt(output, packetId);
        packet.encode(output);
    }
}
