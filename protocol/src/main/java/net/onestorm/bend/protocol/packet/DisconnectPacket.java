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
public class DisconnectPacket implements Packet {

    private static final int MAXIMUM_REASON_LENGTH = 256;

    private int type;
    private String reason;

    @Override
    public void decode(ByteBuf buffer) {
        type = ProtocolUtil.readVarInt(buffer);
        reason = ProtocolUtil.readString(buffer, MAXIMUM_REASON_LENGTH);
    }

    @Override
    public void encode(ByteBuf buffer) {
        ProtocolUtil.writeVarInt(buffer, type);
        ProtocolUtil.writeString(buffer, reason);
    }
}
