/*
 * Copyright (c) 2016-2022 chronicle.software
 *
 *     https://chronicle.software
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.openhft.chronicle.bytes;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;

class Bytes4Test extends BytesTestCommon {

    @Disabled("https://github.com/OpenHFT/Chronicle-Bytes/issues/186")
    @Test
    void segFault() {
        {
            byte[] arr = new byte[4];
            BytesStore<?, byte[]> bs = BytesStore.wrap(arr);
            bs.writeInt(0, 0xaaaaaaaa);
            //System.exit(0);
        }

        BytesStore<?, byte[]> bs = BytesStore.from("this is a test          ");
        bs.append(14, 16, 12);
        bs.isClear();

        assertThrows(RuntimeException.class, () -> bs.writeUtf8(14, "this is a another text it should over write the other"));
    }

    @Test
    @Disabled("https://github.com/OpenHFT/Chronicle-Bytes/issues/187")
    void bufferOverflow() {
        byte[] arr = new byte[4];
        final BytesStore<?, byte[]> bs = BytesStore.wrap(arr);

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
            BytesStore<?, byte[]> bs = BytesStore.wrap(arr);
            bs.writeInt(0, 0xaaaaaaaa);
            //System.exit(0);
        }

        BytesStore<?, byte[]> bs = BytesStore.from("this is a test          ");
        bs.append(14, 16, 12);
        bs.isClear();
        bs.writeUtf8(14, "this is a another text it should over write the other");
        print("ByteStore", bs);

        Bytes<ByteBuffer> by = Bytes.elasticByteBuffer();
        //  ByteBuffer bb = bytes.underlyingObject();
        // System.out.println();
        //  bytes.comment("true").writeBoolean(true);
        by.writeInt(55);
        by.writeHexDumpDescription("new1").writeInt(7);
        by.writeHexDumpDescription("new2").writeInt(8);
        by.writeHexDumpDescription("new2").writeInt(9);
        by.writeHexDumpDescription("new2").writeInt(10);
        //   bytes.comment("new3").writeBigInteger(BigInteger.valueOf(456729));
        print("by", by);

        Bytes<?> b1 = by.bytesForRead();
        Bytes<?> b2 = by.bytesForWrite();
        //  b1.writeInt(2);
        //int I1 = b2.readInt();

        //  System.out.println("I1 = "+I1);

        print("b1 bytesForRead", b1);
        print("b2 bytesForWrite", b2);

    }

    private static void print(String name, BytesStore<?,?> bs) {
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
