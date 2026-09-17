package net.onestorm.bend.protocol;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;
import net.onestorm.bend.protocol.packet.Packet;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public abstract class AbstractProtocol implements Protocol {

    private static final int MAX_PACKET_ID = 0xFF;

    private final ProtocolDirection toClientDirection = new ProtocolDirection();
    private final ProtocolDirection toServerDirection = new ProtocolDirection();

    @Override
    public int getPacketId(Class<? extends Packet> packetClass, Direction direction) {
        final ProtocolDirection protocolDirection = to(direction);
        return protocolDirection.getPacketId(packetClass);
    }

    @Override
    public @Nullable Packet createPacket(int packetId, Direction direction) {
        final ProtocolDirection protocolDirection = to(direction);
        return protocolDirection.createPacket(packetId);
    }

    private ProtocolDirection to(Direction direction) {
        return switch (direction) {
            case TO_CLIENT -> toClientDirection;
            case TO_SERVER -> toServerDirection;
        };
    }

    protected ProtocolDirection toServer() {
        return toServerDirection;
    }

    protected ProtocolDirection toClient() {
        return toClientDirection;
    }

    protected static class ProtocolDirection {

        @SuppressWarnings("unchecked")
        private final @Nullable Supplier<? extends Packet>[] idToConstructor = new Supplier[MAX_PACKET_ID + 1];

        private final Map<Class<? extends Packet>, Integer> classToId = new HashMap<>();

        public int getPacketId(Class<? extends Packet> packetClass) {
            Integer id = classToId.get(packetClass);
            if (id == null) {
                throw new IllegalArgumentException("No packet id registered for " + packetClass.getName());
            }
            return id;
        }

        public @Nullable Packet createPacket(int packetId) {
            if (packetId < 0 || packetId > MAX_PACKET_ID) {
                return null;
            }
            Supplier<? extends Packet> constructor = idToConstructor[packetId];
            if (constructor == null) {
                return null;
            }
            return constructor.get();
        }

        public void registerPacket(
                int packetId, Class<? extends Packet> packetClass, Supplier<? extends Packet> constructor) {
            if (packetId < 0 || packetId > MAX_PACKET_ID) {
                throw new IllegalArgumentException("Packet id out of range: " + packetId);
            }
            idToConstructor[packetId] = constructor;
            classToId.put(packetClass, packetId);
        }
    }
}
