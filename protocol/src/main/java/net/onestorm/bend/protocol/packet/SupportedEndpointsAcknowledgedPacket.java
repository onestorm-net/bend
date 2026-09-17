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
public class SupportedEndpointsAcknowledgedPacket implements Packet {

    private int supportedRevision;

    @Override
    public void decode(ByteBuf buffer) {
        supportedRevision = ProtocolUtil.readVarInt(buffer);
    }

    @Override
    public void encode(ByteBuf buffer) {
        ProtocolUtil.writeVarInt(buffer, supportedRevision);
    }
}
