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
public class HandshakePacket implements Packet {

    private static final int MAXIMUM_IDENTIFIER_LENGTH = 64;

    private int protocolVersionNumber;
    private String clientIdentifier;

    @Override
    public void decode(ByteBuf buffer) {
        protocolVersionNumber = ProtocolUtil.readVarInt(buffer);
        clientIdentifier = ProtocolUtil.readString(buffer, MAXIMUM_IDENTIFIER_LENGTH);
    }

    @Override
    public void encode(ByteBuf buffer) {
        ProtocolUtil.writeVarInt(buffer, protocolVersionNumber);
        ProtocolUtil.writeString(buffer, clientIdentifier);
    }
}
