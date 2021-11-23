package net.openhft.chronicle.bytes;

import org.junit.Ignore;
import org.junit.Test;

import java.nio.ByteBuffer;

import static org.junit.Assert.fail;

public class Bytes4Test {

    @Ignore("https://github.com/OpenHFT/Chronicle-Bytes/issues/186")
    @Test
    public void segFault() {
        {
            byte[] arr = new byte[4];
            BytesStore bs = BytesStore.wrap(arr);
            bs.writeInt(0, 0xaaaaaaaa);
            //System.exit(0);
        }

        BytesStore bs = BytesStore.from("this is a test          ");
        bs.append(14, 16, 12);
        bs.isClear();
        try {
            bs.writeUtf8(14, "this is a another text it should over write the other");
        } catch (Throwable throwable) {
            fail(throwable.getMessage());
        }
    }

    @Test
    @Ignore("https://github.com/OpenHFT/Chronicle-Bytes/issues/187")
    public void bufferOverflow() {
        byte[] arr = new byte[4];
        final BytesStore bs = BytesStore.wrap(arr);

        // Since writeInt can throw AssertionError we need to make it like this
        boolean fail = false;
        try {
            bs.writeInt(-1000, 1);
            fail = true;
        } catch (AssertionError ignore) {
            // Ignore
        }
        if (fail)
            fail("No address range check");

        fail = false;
        try {
            bs.writeInt(4, 2);
            fail = true;
        } catch (AssertionError ignore) {
            // Ignore
        }

        if (fail)
            fail("No address range check");
    }

    public static void main(String[] args) {

        {
            byte[] arr = new byte[4];
            BytesStore bs = BytesStore.wrap(arr);
            bs.writeInt(0, 0xaaaaaaaa);
            //System.exit(0);
        }

        BytesStore bs = BytesStore.from("this is a test          ");
        bs.append(14, 16, 12);
        bs.isClear();
        bs.writeUtf8(14, "this is a another text it should over write the other");
        print("ByteStore", bs);

        Bytes<ByteBuffer> by = Bytes.elasticByteBuffer();
        //  ByteBuffer bb = bytes.underlyingObject();
        // System.out.println();
        //  bytes.comment("true").writeBoolean(true);
        by.writeInt(55);
        by.comment("new1").writeInt(7);
        by.comment("new2").writeInt(8);
        by.comment("new2").writeInt(9);
        by.comment("new2").writeInt(10);
        //   bytes.comment("new3").writeBigInteger(BigInteger.valueOf(456729));
        print("by", by);

        Bytes<?> b1 = by.bytesForRead();
        Bytes b2 = by.bytesForWrite();
        //  b1.writeInt(2);
        //int I1 = b2.readInt();

        //  System.out.println("I1 = "+I1);

        print("b1 bytesForRead", b1);
        print("b2 bytesForWrite", b2);

    }

    private static void print(String name, BytesStore bs) {
        System.out.println("** " + name + " **");
        System.out.println("readlimit  " + bs.readLimit());
        System.out.println("writelimit " + bs.writeLimit());
        System.out.println("readposition " + bs.readPosition());
        System.out.println("writeposition " + bs.writePosition());
        System.out.println("capacity " + bs.capacity());
        System.out.println("real capacity " + bs.realCapacity());
        System.out.println("start " + bs.start());
        System.out.println();

    }
}
