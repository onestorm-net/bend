package net.onestorm.bend.protocol.util;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import java.nio.charset.StandardCharsets;
import net.onestorm.bend.protocol.exception.ProtocolException;

public class ProtocolUtil {

    private static final int DEFAULT_MAXIMUM_STRING_LENGTH = 100_000;

    private static final int MAXIMUM_VAR_INT_SIZE = 5;
    private static final int[] VAR_INT_LENGTHS = new int[33];

    static {
        for (int i = 0; i <= 32; ++i) {
            VAR_INT_LENGTHS[i] = (int) Math.ceil((31d - (i - 1)) / 7d);
        }
        VAR_INT_LENGTHS[32] = 1; // if value is zero, it still needs a byte
    }

    private ProtocolUtil() {}

    public static int varIntLength(int value) {
        return VAR_INT_LENGTHS[Integer.numberOfLeadingZeros(value)];
    }

    public static int readVarInt(ByteBuf buffer) {
        int readable = buffer.readableBytes();
        if (readable == 0) {
            throw new ProtocolException("Cannot read a VarInt from an empty buffer");
        }

        int k = buffer.readByte(); // get first byte

        // if no continue bit, return result k
        if ((k & 0x80) == 0) {
            return k;
        }

        int maxRead = Math.min(MAXIMUM_VAR_INT_SIZE, readable);
        int i = k & 0x7F; // set first 7 bits
        for (int j = 1; j < maxRead; j++) {
            k = buffer.readByte(); // get next byte
            i |= (k & 0x7F) << j * 7; // set next 7 bits
            if ((k & 0x80) == 0) { // if no continue bit, return result i
                return i;
            }
        }
        throw new ProtocolException(
                "VarInt is larger than the maximum supported size (" + MAXIMUM_VAR_INT_SIZE + " bytes)");
    }

    public static int[] readVarIntArray(ByteBuf buffer) {
        int length = ProtocolUtil.readVarInt(buffer);
        validateArrayLength(buffer, length);
        int[] varIntArray = new int[length];
        for (int i = 0; i < length; i++) {
            varIntArray[i] = readVarInt(buffer);
        }
        return varIntArray;
    }

    // every array element (a VarInt, or an Endpoint) takes at least 1 byte on the wire, so a
    // prefixed length claiming more elements than there are readable bytes is never satisfiable;
    // rejecting it here avoids allocating an attacker-controlled-size array before reading it
    public static void validateArrayLength(ByteBuf buffer, int length) {
        if (length < 0) {
            throw new ProtocolException("Negative prefixed array length: " + length);
        }

        if (!buffer.isReadable(length)) {
            throw new ProtocolException("Prefixed array length " + length + " exceeds the " + buffer.readableBytes()
                    + " byte(s) available");
        }
    }

    public static void writeVarInt(ByteBuf buffer, int value) {
        if ((value & (0xFFFFFFFF << 7)) == 0) {
            buffer.writeByte(value);
        } else if ((value & (0xFFFFFFFF << 14)) == 0) {
            int w = (value & 0x7F | 0x80) << 8 | (value >>> 7);
            buffer.writeShort(w);
        } else {
            writeVarIntFull(buffer, value);
        }
    }

    public static void writeVarIntArray(ByteBuf buffer, int[] values) {
        writeVarInt(buffer, values.length);
        for (int value : values) {
            writeVarInt(buffer, value);
        }
    }

    public static void writeVarIntFull(ByteBuf buffer, int value) {
        if ((value & (0xFFFFFFFF << 7)) == 0) {
            buffer.writeByte(value);
        } else if ((value & (0xFFFFFFFF << 14)) == 0) {
            int w = (value & 0x7F | 0x80) << 8 | (value >>> 7);
            buffer.writeShort(w);
        } else if ((value & (0xFFFFFFFF << 21)) == 0) {
            int w = (value & 0x7F | 0x80) << 16 | ((value >>> 7) & 0x7F | 0x80) << 8 | (value >>> 14);
            buffer.writeMedium(w);
        } else if ((value & (0xFFFFFFFF << 28)) == 0) {
            int w = (value & 0x7F | 0x80) << 24
                    | (((value >>> 7) & 0x7F | 0x80) << 16)
                    | ((value >>> 14) & 0x7F | 0x80) << 8
                    | (value >>> 21);
            buffer.writeInt(w);
        } else {
            int w = (value & 0x7F | 0x80) << 24
                    | ((value >>> 7) & 0x7F | 0x80) << 16
                    | ((value >>> 14) & 0x7F | 0x80) << 8
                    | ((value >>> 21) & 0x7F | 0x80);
            buffer.writeInt(w);
            buffer.writeByte(value >>> 28);
        }
    }

    public static String readString(ByteBuf buffer) {
        int length = readVarInt(buffer);
        return readString(buffer, DEFAULT_MAXIMUM_STRING_LENGTH, length);
    }

    public static String readString(ByteBuf buffer, int maximum) {
        int length = readVarInt(buffer);
        return readString(buffer, maximum, length);
    }

    public static String readString(ByteBuf buffer, int maximum, int length) {
        if (length < 0) {
            throw new ProtocolException("Negative prefixed string length: " + length);
        }

        if (length > maximum * 4) {
            throw new ProtocolException("String length " + length + " is higher than the hard limit " + (maximum * 4));
        }

        if (!buffer.isReadable(length)) {
            throw new ProtocolException("Prefixed string length " + length + " exceeds the " + buffer.readableBytes()
                    + " byte(s) available");
        }

        String value = buffer.readString(length, StandardCharsets.UTF_8);

        if (value.length() > maximum) {
            throw new ProtocolException("String length " + value.length() + " is higher than the maximum " + maximum);
        }

        return value;
    }

    public static void writeString(ByteBuf buffer, CharSequence value) {
        int length = ByteBufUtil.utf8Bytes(value);
        writeVarInt(buffer, length);
        buffer.writeCharSequence(value, StandardCharsets.UTF_8);
    }
}
