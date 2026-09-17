package net.onestorm.bend.protocol.packet;

import io.netty.buffer.ByteBuf;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import net.onestorm.bend.protocol.util.ProtocolUtil;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SupportedEndpointsPacket implements Packet {

    private int availableRevision;
    private int supportedRevision;
    private int[] endpointIds;

    @Override
    public void decode(ByteBuf buffer) {
        availableRevision = ProtocolUtil.readVarInt(buffer);
        supportedRevision = ProtocolUtil.readVarInt(buffer);
        endpointIds = ProtocolUtil.readVarIntArray(buffer);
    }

    @Override
    public void encode(ByteBuf buffer) {
        ProtocolUtil.writeVarInt(buffer, availableRevision);
        ProtocolUtil.writeVarInt(buffer, supportedRevision);
        ProtocolUtil.writeVarIntArray(buffer, endpointIds);
    }
}
