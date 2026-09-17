package net.onestorm.bend.protocol.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.onestorm.bend.protocol.exception.ProtocolException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

class ProtocolUtilTest {

    // --- varIntLength ---

    @ParameterizedTest
    @CsvSource({
        "0,          1",
        "1,          1",
        "127,        1",
        "128,        2",
        "16383,      2",
        "16384,      3",
        "2097151,    3",
        "2097152,    4",
        "268435455,  4",
        "268435456,  5",
        "2147483647, 5", // Integer.MAX_VALUE
    })
    void varIntLength_returnsCorrectByteCount(int value, int expected) {
        assertThat(ProtocolUtil.varIntLength(value)).isEqualTo(expected);
    }

    // --- writeVarInt / readVarInt round-trips ---

    @ParameterizedTest
    @ValueSource(
            ints = {
                0,
                1,
                127,
                128,
                16383,
                16384,
                2097151,
                2097152,
                268435455,
                268435456,
                Integer.MAX_VALUE,
                -1,
                -128,
                Integer.MIN_VALUE
            })
    void varInt_roundTrip(int value) {
        ByteBuf buf = Unpooled.buffer();
        try {
            ProtocolUtil.writeVarInt(buf, value);
            assertThat(buf.readableBytes()).isEqualTo(ProtocolUtil.varIntLength(value));
            assertThat(ProtocolUtil.readVarInt(buf)).isEqualTo(value);
            assertThat(buf.isReadable()).isFalse();
        } finally {
            buf.release();
        }
    }

    @Test
    void varInt_zero_encodesAsOneByte() {
        ByteBuf buf = Unpooled.buffer();
        try {
            ProtocolUtil.writeVarInt(buf, 0);
            assertThat(buf.readableBytes()).isEqualTo(1);
            assertThat(buf.getByte(0)).isEqualTo((byte) 0x00);
        } finally {
            buf.release();
        }
    }

    @Test
    void varInt_128_encodesTwoBytes() {
        ByteBuf buf = Unpooled.buffer();
        try {
            ProtocolUtil.writeVarInt(buf, 128);
            assertThat(buf.readableBytes()).isEqualTo(2);
            assertThat(buf.getByte(0)).isEqualTo((byte) 0x80); // low 7 bits + continue
            assertThat(buf.getByte(1)).isEqualTo((byte) 0x01); // remaining bits, no continue
        } finally {
            buf.release();
        }
    }

    // --- writeVarIntFull (every byte-length branch, called directly rather than through
    // writeVarInt's dispatch, since writeVarInt only ever delegates to it for values needing
    // 3+ bytes — the 1-byte and 2-byte branches below are otherwise unreachable) ---

    @ParameterizedTest
    @ValueSource(ints = {0, 1, 127})
    void writeVarIntFull_oneByteValue_roundTrip(int value) {
        assertWriteVarIntFullRoundTrip(value, 1);
    }

    @ParameterizedTest
    @ValueSource(ints = {128, 16383})
    void writeVarIntFull_twoByteValue_roundTrip(int value) {
        assertWriteVarIntFullRoundTrip(value, 2);
    }

    @ParameterizedTest
    @ValueSource(ints = {16384, 2097151})
    void writeVarIntFull_threeByteValue_roundTrip(int value) {
        assertWriteVarIntFullRoundTrip(value, 3);
    }

    @ParameterizedTest
    @ValueSource(ints = {2097152, 268435455})
    void writeVarIntFull_fourByteValue_roundTrip(int value) {
        assertWriteVarIntFullRoundTrip(value, 4);
    }

    @ParameterizedTest
    @ValueSource(ints = {268435456, Integer.MAX_VALUE, -1, -128, Integer.MIN_VALUE})
    void writeVarIntFull_fiveByteValue_roundTrip(int value) {
        assertWriteVarIntFullRoundTrip(value, 5);
    }

    private void assertWriteVarIntFullRoundTrip(int value, int expectedByteLength) {
        ByteBuf buf = Unpooled.buffer();
        try {
            ProtocolUtil.writeVarIntFull(buf, value);
            assertThat(buf.readableBytes()).isEqualTo(expectedByteLength);
            assertThat(ProtocolUtil.readVarInt(buf)).isEqualTo(value);
            assertThat(buf.isReadable()).isFalse();
        } finally {
            buf.release();
        }
    }

    @Test
    void readVarInt_emptyBuffer_throws() {
        ByteBuf buf = Unpooled.buffer();
        try {
            assertThatThrownBy(() -> ProtocolUtil.readVarInt(buf)).isInstanceOf(ProtocolException.class);
        } finally {
            buf.release();
        }
    }

    @Test
    void readVarInt_allFiveBytesContinuation_throws() {
        ByteBuf buf = Unpooled.buffer();
        try {
            buf.writeBytes(new byte[] {(byte) 0x80, (byte) 0x80, (byte) 0x80, (byte) 0x80, (byte) 0x80});
            assertThatThrownBy(() -> ProtocolUtil.readVarInt(buf)).isInstanceOf(ProtocolException.class);
        } finally {
            buf.release();
        }
    }

    // --- writeVarIntArray / readVarIntArray ---

    @Test
    void varIntArray_roundTrip() {
        int[] values = {0, 1, 128, 16384, Integer.MAX_VALUE};
        ByteBuf buf = Unpooled.buffer();
        try {
            ProtocolUtil.writeVarIntArray(buf, values);
            assertThat(ProtocolUtil.readVarIntArray(buf)).isEqualTo(values);
            assertThat(buf.isReadable()).isFalse();
        } finally {
            buf.release();
        }
    }

    @Test
    void varIntArray_empty_roundTrip() {
        ByteBuf buf = Unpooled.buffer();
        try {
            ProtocolUtil.writeVarIntArray(buf, new int[0]);
            assertThat(ProtocolUtil.readVarIntArray(buf)).isEmpty();
            assertThat(buf.isReadable()).isFalse();
        } finally {
            buf.release();
        }
    }

    @Test
    void readVarIntArray_negativePrefixedLength_throws() {
        ByteBuf buf = Unpooled.buffer();
        try {
            // writeVarInt round-trips negative ints as a raw 32-bit pattern (see varInt_roundTrip
            // above), so this is a genuinely reachable prefix, not a hand-crafted impossible input.
            ProtocolUtil.writeVarInt(buf, -1);
            assertThatThrownBy(() -> ProtocolUtil.readVarIntArray(buf)).isInstanceOf(ProtocolException.class);
        } finally {
            buf.release();
        }
    }

    @Test
    void readVarIntArray_lengthExceedsAvailableBytes_throwsWithoutAllocating() {
        ByteBuf buf = Unpooled.buffer();
        try {
            // claims a billion elements but provides none — must be rejected before `new int[length]`
            // is even attempted, since each element needs at least 1 readable byte
            ProtocolUtil.writeVarInt(buf, 1_000_000_000);
            assertThatThrownBy(() -> ProtocolUtil.readVarIntArray(buf)).isInstanceOf(ProtocolException.class);
        } finally {
            buf.release();
        }
    }

    // --- writeString / readString ---

    @Test
    void string_ascii_roundTrip() {
        assertStringRoundTrip("hello world");
    }

    @Test
    void string_empty_roundTrip() {
        assertStringRoundTrip("");
    }

    @Test
    void string_threeBytUtf8_roundTrip() {
        assertStringRoundTrip("こんにちは"); // Japanese, 3-byte UTF-8 per character
    }

    @Test
    void string_fourByteUtf8_roundTrip() {
        assertStringRoundTrip("😀🎉"); // emoji, 4-byte UTF-8 per character
    }

    @Test
    void string_atExactMaxLength_accepted() {
        String value = "a".repeat(64);
        ByteBuf buf = Unpooled.buffer();
        try {
            ProtocolUtil.writeString(buf, value);
            assertThat(ProtocolUtil.readString(buf, 64)).isEqualTo(value);
        } finally {
            buf.release();
        }
    }

    @Test
    void string_overMaxLength_throws() {
        String value = "a".repeat(65);
        ByteBuf buf = Unpooled.buffer();
        try {
            ProtocolUtil.writeString(buf, value);
            assertThatThrownBy(() -> ProtocolUtil.readString(buf, 64)).isInstanceOf(ProtocolException.class);
        } finally {
            buf.release();
        }
    }

    @Test
    void string_fourByteUtf8_overMaxLength_throws() {
        // each emoji is 1 Java char when using codePoints but 2 chars as a String (surrogate pair)
        // 33 emoji = 66 Java chars, over the 64-char limit
        String value = "😀".repeat(33);
        ByteBuf buf = Unpooled.buffer();
        try {
            ProtocolUtil.writeString(buf, value);
            assertThatThrownBy(() -> ProtocolUtil.readString(buf, 64)).isInstanceOf(ProtocolException.class);
        } finally {
            buf.release();
        }
    }

    @Test
    void readString_negativePrefixedLength_throws() {
        ByteBuf buf = Unpooled.buffer();
        try {
            // writeVarInt round-trips negative ints as a raw 32-bit pattern (see varInt_roundTrip
            // above), so this is a genuinely reachable prefix, not a hand-crafted impossible input.
            ProtocolUtil.writeVarInt(buf, -1);
            assertThatThrownBy(() -> ProtocolUtil.readString(buf, 64)).isInstanceOf(ProtocolException.class);
        } finally {
            buf.release();
        }
    }

    @Test
    void readString_lengthOverHardLimit_throws() {
        ByteBuf buf = Unpooled.buffer();
        try {
            // hard limit is maximum * 4; the throw happens before any content bytes are read
            ProtocolUtil.writeVarInt(buf, 65 * 4 + 1);
            assertThatThrownBy(() -> ProtocolUtil.readString(buf, 64)).isInstanceOf(ProtocolException.class);
        } finally {
            buf.release();
        }
    }

    @Test
    void readString_fewerBytesThanPrefixedLength_throws() {
        ByteBuf buf = Unpooled.buffer();
        try {
            ProtocolUtil.writeVarInt(buf, 5);
            buf.writeBytes(new byte[] {'a', 'b'}); // only 2 of the promised 5 bytes are present
            assertThatThrownBy(() -> ProtocolUtil.readString(buf, 64)).isInstanceOf(ProtocolException.class);
        } finally {
            buf.release();
        }
    }

    // --- helpers ---

    private void assertStringRoundTrip(String value) {
        ByteBuf buf = Unpooled.buffer();
        try {
            ProtocolUtil.writeString(buf, value);
            assertThat(ProtocolUtil.readString(buf)).isEqualTo(value);
            assertThat(buf.isReadable()).isFalse();
        } finally {
            buf.release();
        }
    }
}
