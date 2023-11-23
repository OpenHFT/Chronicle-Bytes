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

import net.openhft.chronicle.core.Jvm;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

public class UnsafeRWObjectTest extends BytesTestCommon {
    @Test
    public void longObjectElasticBuffer() {
        assumeTrue(Jvm.is64bit());
        String expected0 = Jvm.isAzulZing() ? "[8, 72]" : "[16, 80]";
        assertEquals(expected0,
                Arrays.toString(BytesUtil.triviallyCopyableRange(BB.class)));

        Bytes<?> directElastic = Bytes.allocateElasticDirect(32);

        BB bb1 = new BB(0L, 1L, 2L, 3L, 4L, 5L, 6L, 7L);

        directElastic.unsafeWriteObject(bb1, 8 * 8);

        BB bb2 = new BB(0L, 0L, 0L, 0L, 0L, 0L, 0L, 0L);

        directElastic.unsafeReadObject(bb2, 8 * 8);

        assertEquals(bb1.l0, bb2.l0);
        assertEquals(bb1.l1, bb2.l1);
        assertEquals(bb1.l2, bb2.l2);
        assertEquals(bb1.l3, bb2.l3);
        assertEquals(bb1.l4, bb2.l4);
        assertEquals(bb1.l5, bb2.l5);
        assertEquals(bb1.l6, bb2.l6);
        assertEquals(bb1.l7, bb2.l7);

        directElastic.releaseLast();
    }

    @Test
    public void shortObject() {
        assumeTrue(Jvm.is64bit());
        String expected0 = Jvm.isAzulZing() ? "[8, 28]" : "[12, 32]";
        assertEquals(expected0,
                Arrays.toString(
                        BytesUtil.triviallyCopyableRange(AA.class)));
        Bytes<?> bytes = Bytes.allocateDirect(32);
        AA aa = new AA(1, 2, 3);
        bytes.unsafeWriteObject(aa, 4 + 2 * 8);
        String expected = Jvm.isAzulZing() ?
                "00000000 02 00 00 00 00 00 00 00  00 00 00 00 00 00 08 40 ········ ·······@\n" +
                        "00000010 01 00 00 00                                      ····             \n"
                : "00000000 01 00 00 00 02 00 00 00  00 00 00 00 00 00 00 00 ········ ········\n" +
                "00000010 00 00 08 40                                      ···@             \n";
        assertEquals(expected, bytes.toHexString());
        AA a2 = new AA(0, 0, 0);
        bytes.unsafeReadObject(a2, 4 + 2 * 8);
        assertEquals(aa.i, a2.i);
        assertEquals(aa.l, a2.l);
        assertEquals(aa.d, a2.d, 0.0);
        bytes.releaseLast();
    }

    @Test
    public void longObject() {
        assumeTrue(Jvm.is64bit());
        String expected0 = Jvm.isAzulZing() ? "[8, 72]" : "[16, 80]";
        int[] ints = BytesUtil.triviallyCopyableRange(BB.class);
        assertEquals(expected0,
                Arrays.toString(ints));
        Bytes<?> bytes = Bytes.allocateDirect(8 * 8);
        BB bb = new BB(0x2000000000000001L, 0x4000000000000003L, 0x6000000000000005L, 0x8000000000000007L, 0xA000000000000009L, 0xC00000000000000BL, 0xE00000000000000DL, 0x100000000000000FL);
        bytes.unsafeWriteObject(bb, ints[0], ints[1] - ints[0]);
        String expected = "" +
                "00000000 01 00 00 00 00 00 00 20  03 00 00 00 00 00 00 40 ·······  ·······@\n" +
                "00000010 05 00 00 00 00 00 00 60  07 00 00 00 00 00 00 80 ·······` ········\n" +
                "00000020 09 00 00 00 00 00 00 a0  0b 00 00 00 00 00 00 c0 ········ ········\n" +
                "00000030 0d 00 00 00 00 00 00 e0  0f 00 00 00 00 00 00 10 ········ ········\n";
        assertEquals(expected, bytes.toHexString());
        BB b2 = new BB(0, 0, 0, 0, 0, 0, 0, 0);
        bytes.unsafeReadObject(b2, ints[0], ints[1] - ints[0]);
        assertEquals(bb, b2);
        Bytes<?> bytes2 = Bytes.allocateElasticOnHeap(8 * 8);
        bytes2.unsafeWriteObject(b2, ints[0], ints[1] - ints[0]);
        assertEquals(expected, bytes2.toHexString());

        bytes.releaseLast();
    }

    @Test
    public void doubleObject() {
        assumeTrue(Jvm.is64bit());
        String expected0 = Jvm.isAzulZing() ? "[8, 72]" : "[16, 80]";
        int[] ints = BytesUtil.triviallyCopyableRange(DD.class);
        assertEquals(expected0,
                Arrays.toString(ints));
        Bytes<?> bytes = Bytes.allocateDirect(8 * 8);
        DD bb = new DD(1.1, 2.2, 3.3, 4.4, 5.5, 6.6, 7.7, 8.8);
        bytes.unsafeWriteObject(bb, BytesUtil.triviallyCopyableRange(DD.class)[0], 8 * 8);
        String expected = "" +
                "00000000 9a 99 99 99 99 99 f1 3f  9a 99 99 99 99 99 01 40 ·······? ·······@\n" +
                "00000010 66 66 66 66 66 66 0a 40  9a 99 99 99 99 99 11 40 ffffff·@ ·······@\n" +
                "00000020 00 00 00 00 00 00 16 40  66 66 66 66 66 66 1a 40 ·······@ ffffff·@\n" +
                "00000030 cd cc cc cc cc cc 1e 40  9a 99 99 99 99 99 21 40 ·······@ ······!@\n";
        assertEquals(expected, bytes.toHexString());
        DD b2 = new DD(0, 0, 0, 0, 0, 0, 0, 0);
        bytes.unsafeReadObject(b2, ints[0], ints[1] - ints[0]);
        assertEquals(bb, b2);
        Bytes<?> bytes2 = Bytes.allocateElasticOnHeap(8 * 8);
        bytes2.unsafeWriteObject(b2, ints[0], ints[1] - ints[0]);
        assertEquals(expected, bytes2.toHexString());

        bytes.releaseLast();
    }

    @Test
    public void arrayByte() {
        assumeTrue(Jvm.is64bit());
        assertEquals("[16]",
                Arrays.toString(
                        BytesUtil.triviallyCopyableRange(byte[].class)));
        Bytes<?> bytes = Bytes.allocateDirect(32);
        byte[] byteArray = "Hello World.".getBytes();
        bytes.unsafeWriteObject(byteArray, byteArray.length);
        assertEquals("00000000 48 65 6c 6c 6f 20 57 6f  72 6c 64 2e             Hello Wo rld.    \n",
                bytes.toHexString());
        byte[] byteArray2 = new byte[byteArray.length];
        bytes.unsafeReadObject(byteArray2, byteArray.length);
        assertArrayEquals(byteArray, byteArray2);

        assertEquals("Hello World.", new String(byteArray2));
        bytes.releaseLast();

    }

    @Test
    public void arrayInt() {
        assumeTrue(Jvm.is64bit());
        assertEquals("[16]",
                Arrays.toString(
                        BytesUtil.triviallyCopyableRange(int[].class)));
        Bytes<?> bytes = Bytes.allocateDirect(32);
        int[] array = new int[]{1, 2, 4, 3};
        bytes.unsafeWriteObject(array, 4 * 4);
        assertEquals("00000000 01 00 00 00 02 00 00 00  04 00 00 00 03 00 00 00 ········ ········\n",
                bytes.toHexString());
        int[] array2 = new int[array.length];
        bytes.unsafeReadObject(array2, 4 * 4);
        assertArrayEquals(array, array2);
        bytes.releaseLast();

    }

    static class AA {
        int i;
        long l;
        double d;

        public AA(int i, long l, double d) {
            this.i = i;
            this.l = l;
            this.d = d;
        }
    }

    static class BB {
        long l0, l1, l2, l3, l4, l5, l6, l7;

        public BB(long l0, long l1, long l2, long l3, long l4, long l5, long l6, long l7) {
            this.l0 = l0;
            this.l1 = l1;
            this.l2 = l2;
            this.l3 = l3;
            this.l4 = l4;
            this.l5 = l5;
            this.l6 = l6;
            this.l7 = l7;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            BB bb = (BB) o;
            return l0 == bb.l0 && l1 == bb.l1 && l2 == bb.l2 && l3 == bb.l3 && l4 == bb.l4 && l5 == bb.l5 && l6 == bb.l6 && l7 == bb.l7;
        }
    }

    static class DD {
        double l0, l1, l2, l3, l4, l5, l6, l7;

        public DD(double l0, double l1, double l2, double l3, double l4, double l5, double l6, double l7) {
            this.l0 = l0;
            this.l1 = l1;
            this.l2 = l2;
            this.l3 = l3;
            this.l4 = l4;
            this.l5 = l5;
            this.l6 = l6;
            this.l7 = l7;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            DD dd = (DD) o;
            return Double.compare(l0, dd.l0) == 0 && Double.compare(l1, dd.l1) == 0 && Double.compare(l2, dd.l2) == 0 && Double.compare(l3, dd.l3) == 0 && Double.compare(l4, dd.l4) == 0 && Double.compare(l5, dd.l5) == 0 && Double.compare(l6, dd.l6) == 0 && Double.compare(l7, dd.l7) == 0;
        }
    }
}
