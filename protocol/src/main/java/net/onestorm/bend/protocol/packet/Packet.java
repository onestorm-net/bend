package net.onestorm.bend.protocol.packet;

import io.netty.buffer.ByteBuf;

public interface Packet {

    void decode(ByteBuf buffer) throws Exception;

    void encode(ByteBuf buffer) throws Exception;
}
