package net.onestorm.bend.protocol.packet;

import io.netty.buffer.ByteBuf;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import net.onestorm.bend.protocol.endpoint.Endpoint;
import net.onestorm.bend.protocol.util.ProtocolUtil;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AvailableEndpointsPacket implements Packet {

    private static final int MAXIMUM_ENDPOINT_NAME_LENGTH = 64;

    private int availableRevision;
    private Endpoint[] endpoints;

    @Override
    public void decode(ByteBuf buffer) {
        availableRevision = ProtocolUtil.readVarInt(buffer);
        endpoints = readEndpointArray(buffer);
    }

    @Override
    public void encode(ByteBuf buffer) {
        ProtocolUtil.writeVarInt(buffer, availableRevision);
        writeEndpointArray(buffer, endpoints);
    }

    private Endpoint[] readEndpointArray(ByteBuf buffer) {
        int length = ProtocolUtil.readVarInt(buffer);
        ProtocolUtil.validateArrayLength(buffer, length);
        Endpoint[] endpointArray = new Endpoint[length];
        for (int i = 0; i < length; i++) {
            endpointArray[i] = readEndpoint(buffer);
        }
        return endpointArray;
    }

    private Endpoint readEndpoint(ByteBuf buffer) {
        int id = ProtocolUtil.readVarInt(buffer);
        String name = ProtocolUtil.readString(buffer, MAXIMUM_ENDPOINT_NAME_LENGTH);
        int flags = ProtocolUtil.readVarInt(buffer); // currently always 1 byte
        return new Endpoint(id, name, flags);
    }

    private void writeEndpointArray(ByteBuf buffer, Endpoint[] endpoints) {
        ProtocolUtil.writeVarInt(buffer, endpoints.length);
        for (Endpoint endpoint : endpoints) {
            writeEndpoint(buffer, endpoint);
        }
    }

    private void writeEndpoint(ByteBuf buffer, Endpoint endpoint) {
        ProtocolUtil.writeVarInt(buffer, endpoint.getId());
        ProtocolUtil.writeString(buffer, endpoint.getName());
        ProtocolUtil.writeVarInt(buffer, endpoint.getFlags());
    }
}
