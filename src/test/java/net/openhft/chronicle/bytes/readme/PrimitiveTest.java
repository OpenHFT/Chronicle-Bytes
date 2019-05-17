package net.openhft.chronicle.bytes.readme;

import net.openhft.chronicle.bytes.*;
import org.junit.Test;

import java.nio.ByteBuffer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assume.assumeFalse;

public class PrimitiveTest {
    @Test
    public void testBinaryPrimitiveDTO() {
        PrimitiveDTO dto = new PrimitiveDTO(true,
                (byte) 0x11,
                (short) 0x2222,
                '5',
                0x12345678,
                0x123456789ABCDEFL,
                1.2345f,
                Math.PI);

        HexDumpBytes bytes = new HexDumpBytes();
        bytes.comment("dto");
        dto.writeMarshallable(bytes);
        System.out.println(bytes.toHexString());

        PrimitiveDTO dto2 = new PrimitiveDTO();
        dto2.readMarshallable(bytes);
        bytes.release();
    }

    @Test
    public void testBinaryPrimitive() {
        HexDumpBytes bytes = new HexDumpBytes();
        bytes.comment("flag").writeBoolean(true);
        bytes.comment("s8").writeByte((byte) 1);
        bytes.comment("u8").writeUnsignedByte(2);
        bytes.comment("s16").writeShort((short) 3);
        bytes.comment("u16").writeUnsignedShort(4);
        bytes.comment("ch").writeStopBit('5');
        bytes.comment("s24").writeInt24(-6_666_666);
        bytes.comment("u24").writeUnsignedInt24(16_666_666);
        bytes.comment("s32").writeInt(6);
        bytes.comment("u32").writeUnsignedInt(7);
        bytes.comment("s64").writeLong(8);
        bytes.comment("f32").writeFloat(9);
        bytes.comment("f64").writeDouble(10);

        System.out.println(bytes.toHexString());

        boolean flag = bytes.readBoolean();
        byte s8 = bytes.readByte();
        int u8 = bytes.readUnsignedByte();
        short s16 = bytes.readShort();
        int u16 = bytes.readUnsignedShort();
        char ch = bytes.readStopBitChar();
        int s24 = bytes.readInt24();
        long u24 = bytes.readUnsignedInt24();
        int s32 = bytes.readInt();
        long u32 = bytes.readUnsignedInt();
        long s64 = bytes.readLong();
        float f32 = bytes.readFloat();
        double f64 = bytes.readDouble();

        assertEquals(true, flag);
        assertEquals(1, s8);
        assertEquals(2, u8);
        assertEquals(3, s16);
        assertEquals(4, u16);
        assertEquals('5', ch);
        assertEquals(-6_666_666, s24);
        assertEquals(16_666_666, u24);
        assertEquals(6, s32);
        assertEquals(7, u32);
        assertEquals(8, s64);
        assertEquals(9, f32, 0.0);
        assertEquals(10, f64, 0.0);
        bytes.release();
    }

    static class PrimitiveDTO implements BytesMarshallable {
        boolean flag;
        byte s8;
        short s16;
        char ch;
        int s32;
        long s64;
        float f32;
        double f64;

        public PrimitiveDTO(boolean flag, byte s8, short s16, char ch, int s32, long s64, float f32, double f64) {
            this.flag = flag;
            this.s8 = s8;
            this.s16 = s16;
            this.ch = ch;
            this.s32 = s32;
            this.s64 = s64;
            this.f32 = f32;
            this.f64 = f64;
        }

        public PrimitiveDTO() {
        }
    }

    @Test
    public void testBinaryPrimitiveOffset() {
        Bytes<ByteBuffer> bytes = Bytes.elasticHeapByteBuffer(64);
        bytes.writeBoolean(0, true);
        bytes.writeByte(1, (byte) 1);
        bytes.writeUnsignedByte(2, 2);
        bytes.writeShort(3, (short) 3);
        bytes.writeUnsignedShort(5, 4);
        bytes.writeInt(7, 6);
        bytes.writeUnsignedInt(11, 7);
        bytes.writeLong(15, 8);
        bytes.writeFloat(23, 9);
        bytes.writeDouble(27, 10);
        bytes.writePosition(35);

        System.out.println(bytes.toHexString());

        boolean flag = bytes.readBoolean(0);
        byte s8 = bytes.readByte(1);
        int u8 = bytes.readUnsignedByte(2);
        short s16 = bytes.readShort(3);
        int u16 = bytes.readUnsignedShort(5);
        int s32 = bytes.readInt(7);
        long u32 = bytes.readUnsignedInt(11);
        long s64 = bytes.readLong(15);
        float f32 = bytes.readFloat(23);
        double f64 = bytes.readDouble(27);

        assertEquals(true, flag);
        assertEquals(1, s8);
        assertEquals(2, u8);
        assertEquals(3, s16);
        assertEquals(4, u16);
        assertEquals(6, s32);
        assertEquals(7, u32);
        assertEquals(8, s64);
        assertEquals(9, f32, 0.0);
        assertEquals(10, f64, 0.0);
    }

    @Test
    public void testTextPrimitive() {
        assumeFalse(NativeBytes.areNewGuarded());
        Bytes<ByteBuffer> bytes = Bytes.elasticHeapByteBuffer(64);
        bytes.append(true).append('\n');
        bytes.append(1).append('\n');
        bytes.append(2L).append('\n');
        bytes.append('3').append('\n');
        bytes.append(4.1f).append('\n');
        bytes.append(5.2).append('\n');
        bytes.append(6.2999999, 3).append('\n');

        System.out.println(bytes.toHexString());

        boolean flag = bytes.parseBoolean();
        int s32 = bytes.parseInt();
        long s64 = bytes.parseLong();
        String ch = bytes.parseUtf8(StopCharTesters.SPACE_STOP);
        float f32 = bytes.parseFloat();
        double f64 = bytes.parseDouble();
        double f64b = bytes.parseDouble();

        assertEquals(true, flag);
        assertEquals(1, s32);
        assertEquals(2, s64);
        assertEquals("3", ch);
        assertEquals(4.1, f32, 1e-6);
        assertEquals(5.2, f64, 0.0);
        assertEquals(6.2999999, f64b, 0.5e-4);
    }
}
