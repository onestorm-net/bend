package net.onestorm.bend.protocol.packet;

import static org.assertj.core.api.Assertions.assertThat;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.jupiter.api.Test;

class EndpointMessagePacketTest {

    @Test
    void endpointMessage_roundTrip() {
        ByteBuf payload = Unpooled.buffer();
        payload.writeBytes(new byte[] {1, 2, 3, 4, 5});

        ByteBuf buf = Unpooled.buffer();
        try {
            new EndpointMessagePacket(42, payload).encode(buf);

            EndpointMessagePacket decoded = new EndpointMessagePacket();
            decoded.decode(buf);

            assertThat(decoded.getId()).isEqualTo(42);
            assertThat(decoded.content()).isEqualTo(Unpooled.wrappedBuffer(new byte[] {1, 2, 3, 4, 5}));
            assertThat(buf.isReadable()).isFalse();

            decoded.release();
        } finally {
            buf.release();
            payload.release();
        }
    }

    @Test
    void endpointMessage_emptyPayload_roundTrip() {
        ByteBuf buf = Unpooled.buffer();
        try {
            new EndpointMessagePacket(1, Unpooled.EMPTY_BUFFER).encode(buf);

            EndpointMessagePacket decoded = new EndpointMessagePacket();
            decoded.decode(buf);

            assertThat(decoded.getId()).isEqualTo(1);
            assertThat(decoded.content().isReadable()).isFalse();

            decoded.release();
        } finally {
            buf.release();
        }
    }

    @Test
    void endpointMessage_decode_retainsPayload() {
        ByteBuf payload = Unpooled.buffer();
        payload.writeBytes(new byte[] {1, 2, 3});

        ByteBuf buf = Unpooled.buffer();
        new EndpointMessagePacket(1, payload).encode(buf);
        payload.release();

        EndpointMessagePacket decoded = new EndpointMessagePacket();
        decoded.decode(buf);
        buf.release(); // releasing the source buffer should not affect the decoded payload

        assertThat(decoded.content().isReadable()).isTrue();
        assertThat(decoded.content().readableBytes()).isEqualTo(3);

        decoded.release();
    }

    @Test
    void endpointMessage_setPayload_releasesOldBuffer() {
        ByteBuf oldPayload = Unpooled.buffer();
        oldPayload.writeByte(1);

        EndpointMessagePacket packet = new EndpointMessagePacket(1, oldPayload);
        assertThat(oldPayload.refCnt()).isEqualTo(1);

        ByteBuf newPayload = Unpooled.buffer();
        newPayload.writeByte(2);
        packet.setPayload(newPayload);

        assertThat(oldPayload.refCnt()).isEqualTo(0);
        assertThat(packet.content()).isSameAs(newPayload);

        newPayload.release();
    }

    @Test
    void copy_createsIndependentPacketWithCopiedPayload() {
        ByteBuf payload = Unpooled.buffer();
        payload.writeBytes(new byte[] {1, 2, 3});

        EndpointMessagePacket packet = new EndpointMessagePacket(1, payload);
        EndpointMessagePacket copy = (EndpointMessagePacket) packet.copy();

        assertThat(copy.getId()).isEqualTo(1);
        assertThat(copy.content()).isEqualTo(payload);
        assertThat(copy.content()).isNotSameAs(payload);

        packet.release();
        copy.release();
    }

    @Test
    void duplicate_sharesPayloadContent() {
        ByteBuf payload = Unpooled.buffer();
        payload.writeBytes(new byte[] {1, 2, 3});

        EndpointMessagePacket packet = new EndpointMessagePacket(1, payload);
        EndpointMessagePacket duplicate = (EndpointMessagePacket) packet.duplicate();

        assertThat(duplicate.content()).isEqualTo(payload);

        packet.release();
    }

    @Test
    void retainedDuplicate_incrementsRefCount() {
        ByteBuf payload = Unpooled.buffer();
        payload.writeBytes(new byte[] {1, 2, 3});

        EndpointMessagePacket packet = new EndpointMessagePacket(1, payload);
        EndpointMessagePacket duplicate = (EndpointMessagePacket) packet.retainedDuplicate();

        assertThat(payload.refCnt()).isEqualTo(2);

        packet.release();
        duplicate.release();
    }

    @Test
    void replace_swapsContentAndReleasesOld() {
        ByteBuf oldPayload = Unpooled.buffer();
        oldPayload.writeByte(1);
        ByteBuf newPayload = Unpooled.buffer();
        newPayload.writeByte(2);

        EndpointMessagePacket packet = new EndpointMessagePacket(1, oldPayload);
        EndpointMessagePacket replaced = (EndpointMessagePacket) packet.replace(newPayload);

        assertThat(oldPayload.refCnt()).isEqualTo(0);
        assertThat(replaced).isSameAs(packet);
        assertThat(replaced.content()).isSameAs(newPayload);

        newPayload.release();
    }

    @Test
    void refCnt_reflectsPayloadRefCount() {
        ByteBuf payload = Unpooled.buffer();
        EndpointMessagePacket packet = new EndpointMessagePacket(1, payload);

        assertThat(packet.refCnt()).isEqualTo(1);
        packet.release();
        assertThat(packet.refCnt()).isEqualTo(0);
    }

    @Test
    void retain_incrementsRefCountAndReturnsSelf() {
        ByteBuf payload = Unpooled.buffer();
        EndpointMessagePacket packet = new EndpointMessagePacket(1, payload);

        assertThat(packet.retain()).isSameAs(packet);
        assertThat(payload.refCnt()).isEqualTo(2);

        payload.release(2);
    }

    @Test
    void retainWithIncrement_incrementsRefCountByGivenAmount() {
        ByteBuf payload = Unpooled.buffer();
        EndpointMessagePacket packet = new EndpointMessagePacket(1, payload);

        assertThat(packet.retain(3)).isSameAs(packet);
        assertThat(payload.refCnt()).isEqualTo(4);

        payload.release(4);
    }

    @Test
    void touch_returnsSelf() {
        ByteBuf payload = Unpooled.buffer();
        EndpointMessagePacket packet = new EndpointMessagePacket(1, payload);

        assertThat(packet.touch()).isSameAs(packet);

        packet.release();
    }

    @Test
    void touchWithHint_returnsSelf() {
        ByteBuf payload = Unpooled.buffer();
        EndpointMessagePacket packet = new EndpointMessagePacket(1, payload);

        assertThat(packet.touch("debug hint")).isSameAs(packet);

        packet.release();
    }

    @Test
    void releaseWithDecrement_releasesGivenAmountAndReportsFullyReleased() {
        ByteBuf payload = Unpooled.buffer();
        payload.retain(); // refCnt 2
        EndpointMessagePacket packet = new EndpointMessagePacket(1, payload);

        assertThat(packet.release(2)).isTrue();
        assertThat(payload.refCnt()).isEqualTo(0);
    }
}
