package net.onestorm.bend.protocol.packet;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufHolder;
import io.netty.buffer.Unpooled;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import net.onestorm.bend.protocol.util.ProtocolUtil;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class EndpointMessagePacket implements Packet, ByteBufHolder {

    private int id;
    private ByteBuf payload = Unpooled.EMPTY_BUFFER;

    @Override
    public void decode(ByteBuf buffer) {
        id = ProtocolUtil.readVarInt(buffer);
        payload = buffer.readRetainedSlice(buffer.readableBytes());
    }

    @Override
    public void encode(ByteBuf buffer) {
        ProtocolUtil.writeVarInt(buffer, id);
        buffer.writeBytes(payload, payload.readerIndex(), payload.readableBytes());
    }

    // blocks generated setter from lombok to fix possible memory leak
    public void setPayload(ByteBuf payload) {
        this.payload.release();
        this.payload = payload;
    }

    @Override
    public ByteBuf content() {
        return payload;
    }

    @Override
    public ByteBufHolder copy() {
        return new EndpointMessagePacket(id, payload.copy());
    }

    @Override
    public ByteBufHolder duplicate() {
        return new EndpointMessagePacket(id, payload.duplicate());
    }

    @Override
    public ByteBufHolder retainedDuplicate() {
        return new EndpointMessagePacket(id, payload.retainedDuplicate());
    }

    @Override
    public ByteBufHolder replace(ByteBuf content) {
        payload.release();
        payload = content;
        return this;
    }

    @Override
    public int refCnt() {
        return payload.refCnt();
    }

    @Override
    public ByteBufHolder retain() {
        payload.retain();
        return this;
    }

    @Override
    public ByteBufHolder retain(int increment) {
        payload.retain(increment);
        return this;
    }

    @Override
    public ByteBufHolder touch() {
        payload.touch();
        return this;
    }

    @Override
    public ByteBufHolder touch(Object hint) {
        payload.touch(hint);
        return this;
    }

    @Override
    public boolean release() {
        return payload.release();
    }

    @Override
    public boolean release(int i) {
        return payload.release(i);
    }
}
