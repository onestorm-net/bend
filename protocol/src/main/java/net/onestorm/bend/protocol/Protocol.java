package net.onestorm.bend.protocol;

import net.onestorm.bend.protocol.packet.Packet;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public interface Protocol {

    enum Direction {
        TO_CLIENT,
        TO_SERVER
    }

    int getVersionNumber();

    int getPacketId(Class<? extends Packet> packetClass, Direction direction);

    @Nullable
    Packet createPacket(int packetId, Direction direction);
}
