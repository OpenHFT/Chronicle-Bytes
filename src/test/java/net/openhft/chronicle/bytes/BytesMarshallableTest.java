/*
 * Copyright 2016 higherfrequencytrading.com
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package net.openhft.chronicle.bytes;

import net.openhft.chronicle.core.Jvm;
import net.openhft.chronicle.core.io.IORuntimeException;
import net.openhft.chronicle.core.annotation.NotNull;
import org.junit.After;
import org.junit.Test;

import java.io.IOException;
import java.lang.annotation.RetentionPolicy;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.util.*;

import static org.junit.Assert.assertEquals;

/*
 * Created by Peter Lawrey on 20/04/2016.
 */
public class BytesMarshallableTest {

    @After
    public void checkRegisteredBytes() {
        BytesUtil.checkRegisteredBytes();
    }

    @Test
    public void serializePrimitives() throws IORuntimeException {
        Bytes<?> bytes = new HexDumpBytes();
        @NotNull MyByteable mb1 = new MyByteable(false, (byte) 1, (short) 2, '3', 4, 5.5f, 6, 7.7);
        @NotNull MyByteable mb2 = new MyByteable(true, (byte) 11, (short) 22, 'T', 44, 5.555f, 66, 77.77);
        bytes.comment("mb1").writeUnsignedByte(1);
        mb1.writeMarshallable(bytes);
        bytes.comment("mb2").writeUnsignedByte(2);
        mb2.writeMarshallable(bytes);

        assertEquals(
                "01                                              # mb1\n" +
                        "   4e                                              # flag\n" +
                        "   01                                              # b\n" +
                        "   02 00                                           # s\n" +
                        "   33                                              # c\n" +
                        "   04 00 00 00                                     # i\n" +
                        "   00 00 b0 40                                     # f\n" +
                        "   06 00 00 00 00 00 00 00                         # l\n" +
                        "   cd cc cc cc cc cc 1e 40                         # d\n" +
                        "02                                              # mb2\n" +
                        "   59                                              # flag\n" +
                        "   0b                                              # b\n" +
                        "   16 00                                           # s\n" +
                        "   54                                              # c\n" +
                        "   2c 00 00 00                                     # i\n" +
                        "   8f c2 b1 40                                     # f\n" +
                        "   42 00 00 00 00 00 00 00                         # l\n" +
                        "   e1 7a 14 ae 47 71 53 40                         # d\n", bytes.toHexString());

        @NotNull MyByteable mb3 = new MyByteable();
        @NotNull MyByteable mb4 = new MyByteable();
        assertEquals(1, bytes.readUnsignedByte());
        mb3.readMarshallable(bytes);
        assertEquals(2, bytes.readUnsignedByte());
        mb4.readMarshallable(bytes);

        assertEquals(mb1.toString(), mb3.toString());
        assertEquals(mb2.toString(), mb4.toString());
        bytes.release();
    }

    @SuppressWarnings("rawtypes")
    @Test
    public void serializeScalars() throws IORuntimeException {
        Bytes<?> bytes = new HexDumpBytes();
        @NotNull MyScalars mb1 = new MyScalars("Hello", BigInteger.ONE, BigDecimal.TEN, LocalDate.now(), LocalTime.now(), LocalDateTime.now(), ZonedDateTime.now(), UUID.randomUUID());
        @NotNull MyScalars mb2 = new MyScalars("World", BigInteger.ZERO, BigDecimal.ZERO, LocalDate.now(), LocalTime.now(), LocalDateTime.now(), ZonedDateTime.now(), UUID.randomUUID());
        bytes.comment("mb1").writeUnsignedByte(1);
        mb1.writeMarshallable(bytes);
        bytes.comment("mb2").writeUnsignedByte(2);
        mb2.writeMarshallable(bytes);

/*
        assertEquals(
                "01                                              # mb1\n" +
                        "   05 48 65 6c 6c 6f                               # s\n" +
                        "   01 31                                           # bi\n" +
                        "   02 31 30                                        # bd\n" +
                        "   0a 32 30 31 37 2d 31 31 2d 30 36                # date\n" +
                        "   0c 31 32 3a 32 37 3a 34 34 2e 33 33 30          # time\n" +
                        "   17 32 30 31 37 2d 31 31 2d 30 36 54 31 32 3a 32 # dateTime\n" +
                        "   37 3a 34 34 2e 33 33 30 27 32 30 31 37 2d 31 31 # zonedDateTime\n" +
                        "   2d 30 36 54 31 32 3a 32 37 3a 34 34 2e 33 33 31\n" +
                        "   5a 5b 45 75 72 6f 70 65 2f 4c 6f 6e 64 6f 6e 5d # uuid\n" +
                        "   24 63 35 66 33 34 62 39 63 2d 36 34 35 34 2d 34\n" +
                        "   62 61 63 2d 61 32 66 37 2d 66 37 31 36 35 32 33\n" +
                        "   62 62 32 64 33\n" +
                        "02                                              # mb2\n" +
                        "   05 57 6f 72 6c 64                               # s\n" +
                        "   01 30                                           # bi\n" +
                        "   01 30                                           # bd\n" +
                        "   0a 32 30 31 37 2d 31 31 2d 30 36                # date\n" +
                        "   0c 31 32 3a 32 37 3a 34 34 2e 33 33 36          # time\n" +
                        "   17 32 30 31 37 2d 31 31 2d 30 36 54 31 32 3a 32 # dateTime\n" +
                        "   37 3a 34 34 2e 33 33 36 27 32 30 31 37 2d 31 31 # zonedDateTime\n" +
                        "   2d 30 36 54 31 32 3a 32 37 3a 34 34 2e 33 33 36\n" +
                        "   5a 5b 45 75 72 6f 70 65 2f 4c 6f 6e 64 6f 6e 5d # uuid\n" +
                        "   24 32 65 61 35 66 33 34 35 2d 36 65 38 30 2d 34\n" +
                        "   35 66 30 2d 62 66 62 64 2d 63 33 30 37 34 34 33\n" +
                        "   65 32 38 61 34\n", bytes.toHexString());
*/
        Bytes bytes2 = HexDumpBytes.fromText(bytes.toHexString());
        for (int i = 0; i < 2; i++) {
            @NotNull MyScalars mb3 = new MyScalars();
            @NotNull MyScalars mb4 = new MyScalars();
            assertEquals(1, bytes.readUnsignedByte());
            mb3.readMarshallable(bytes);
            assertEquals(2, bytes.readUnsignedByte());
            mb4.readMarshallable(bytes);

            assertEquals(mb1.toString(), mb3.toString());
            assertEquals(mb2.toString(), mb4.toString());

            bytes.release();

            bytes = bytes2;
        }
    }

    @Test
    public void serializeNested() throws IORuntimeException {
        Bytes<?> bytes = new HexDumpBytes();

        @NotNull MyByteable mb1 = new MyByteable(false, (byte) 1, (short) 2, '3', 4, 5.5f, 6, 7.7);
        @NotNull MyByteable mb2 = new MyByteable(true, (byte) 11, (short) 22, 'T', 44, 5.555f, 66, 77.77);
        ZonedDateTime zdt1 = ZonedDateTime.parse("2017-11-06T12:35:56.775Z[Europe/London]");
        ZonedDateTime zdt2 = ZonedDateTime.parse("2016-10-05T01:34:56.775Z[Europe/London]");
        UUID uuid1 = new UUID(0x123456789L, 0xABCDEF);
        UUID uuid2 = new UUID(0x1111111111111111L, 0x2222222222222222L);
        @NotNull MyScalars ms1 = new MyScalars("Hello", BigInteger.ONE, BigDecimal.TEN, zdt1.toLocalDate(), zdt1.toLocalTime(), zdt1.toLocalDateTime(), zdt1, uuid1);
        @NotNull MyScalars ms2 = new MyScalars("World", BigInteger.ZERO, BigDecimal.ZERO, zdt2.toLocalDate(), zdt2.toLocalTime(), zdt2.toLocalDateTime(), zdt2, uuid2);
        @NotNull MyNested mn1 = new MyNested(mb1, ms1);
        @NotNull MyNested mn2 = new MyNested(mb2, ms2);
        bytes.comment("mn1").writeUnsignedByte(1);
        mn1.writeMarshallable(bytes);
        bytes.comment("mn2").writeUnsignedByte(2);
        mn2.writeMarshallable(bytes);

        final String expected = "01                                              # mn1\n" +
                "                                                # byteable\n" +
                "      4e                                              # flag\n" +
                "      01                                              # b\n" +
                "      02 00                                           # s\n" +
                "      33                                              # c\n" +
                "      04 00 00 00                                     # i\n" +
                "      00 00 b0 40                                     # f\n" +
                "      06 00 00 00 00 00 00 00                         # l\n" +
                "      cd cc cc cc cc cc 1e 40                         # d\n" +
                "                                                # scalars\n" +
                "      05 48 65 6c 6c 6f                               # s\n" +
                "      01 31                                           # bi\n" +
                "      02 31 30                                        # bd\n" +
                "      0a 32 30 31 37 2d 31 31 2d 30 36                # date\n" +
                "      0c 31 32 3a 33 35 3a 35 36 2e 37 37 35          # time\n" +
                "      17 32 30 31 37 2d 31 31 2d 30 36 54 31 32 3a 33 # dateTime\n" +
                "      35 3a 35 36 2e 37 37 35 27 32 30 31 37 2d 31 31 # zonedDateTime\n" +
                "      2d 30 36 54 31 32 3a 33 35 3a 35 36 2e 37 37 35\n" +
                "      5a 5b 45 75 72 6f 70 65 2f 4c 6f 6e 64 6f 6e 5d # uuid\n" +
                "      24 30 30 30 30 30 30 30 31 2d 32 33 34 35 2d 36\n" +
                "      37 38 39 2d 30 30 30 30 2d 30 30 30 30 30 30 61\n" +
                "      62 63 64 65 66\n" +
                "02                                              # mn2\n" +
                "                                                # byteable\n" +
                "      59                                              # flag\n" +
                "      0b                                              # b\n" +
                "      16 00                                           # s\n" +
                "      54                                              # c\n" +
                "      2c 00 00 00                                     # i\n" +
                "      8f c2 b1 40                                     # f\n" +
                "      42 00 00 00 00 00 00 00                         # l\n" +
                "      e1 7a 14 ae 47 71 53 40                         # d\n" +
                "                                                # scalars\n" +
                "      05 57 6f 72 6c 64                               # s\n" +
                "      01 30                                           # bi\n" +
                "      01 30                                           # bd\n" +
                "      0a 32 30 31 36 2d 31 30 2d 30 35                # date\n" +
                (Jvm.isJava9Plus() ?
                        "      0c 30 32 3a 33 34 3a 35 36 2e 37 37 35          # time\n" :
                        "      0c 30 31 3a 33 34 3a 35 36 2e 37 37 35          # time\n") +
                (Jvm.isJava9Plus() ?
                        "      17 32 30 31 36 2d 31 30 2d 30 35 54 30 32 3a 33 # dateTime\n" :
                        "      17 32 30 31 36 2d 31 30 2d 30 35 54 30 31 3a 33 # dateTime\n") +
                "      34 3a 35 36 2e 37 37 35 2c 32 30 31 36 2d 31 30 # zonedDateTime\n" +
                (Jvm.isJava9Plus() ?
                        "      2d 30 35 54 30 32 3a 33 34 3a 35 36 2e 37 37 35\n" :
                        "      2d 30 35 54 30 31 3a 33 34 3a 35 36 2e 37 37 35\n") +
                "      2b 30 31 3a 30 30 5b 45 75 72 6f 70 65 2f 4c 6f\n" +
                "      6e 64 6f 6e 5d 24 31 31 31 31 31 31 31 31 2d 31 # uuid\n" +
                "      31 31 31 2d 31 31 31 31 2d 32 32 32 32 2d 32 32\n" +
                "      32 32 32 32 32 32 32 32 32 32\n";

        System.out.println(bytes.toHexString());

        assertEquals(
                expected, bytes.toHexString());

        @NotNull MyNested mn3 = new MyNested();
        @NotNull MyNested mn4 = new MyNested();
        assertEquals(1, bytes.readUnsignedByte());
        mn3.readMarshallable(bytes);
        assertEquals(2, bytes.readUnsignedByte());
        mn4.readMarshallable(bytes);

        assertEquals(mn1.toString(), mn3.toString());
        assertEquals(mn2.toString(), mn4.toString());
        bytes.release();
    }

    @SuppressWarnings("rawtypes")
    @Test
    public void serializeBytes() throws IOException {
        Bytes<?> bytes = new HexDumpBytes();
        try (@NotNull MyBytes mb1 = new MyBytes(Bytes.from("hello"), Bytes.allocateElasticDirect().append("2"));
             @NotNull MyBytes mb2 = new MyBytes(Bytes.from("byeee"), null)) {
            bytes.comment("mb1").writeUnsignedByte(1);
            mb1.writeMarshallable(bytes);
            bytes.comment("mb2").writeUnsignedByte(2);
            mb2.writeMarshallable(bytes);

            Bytes bytes2 = HexDumpBytes.fromText(bytes.toHexString());
            for (int i = 0; i < 2; i++) {
                try (@NotNull MyBytes mb3 = new MyBytes();
                     @NotNull MyBytes mb4 = new MyBytes(Bytes.from("already"), Bytes.allocateElasticDirect().append("value"))) {
                    assertEquals(1, bytes.readUnsignedByte());
                    mb3.readMarshallable(bytes);
                    assertEquals(2, bytes.readUnsignedByte());
                    mb4.readMarshallable(bytes);

                    assertEquals(mb1.toString(), mb3.toString());
                    assertEquals(mb2.toString(), mb4.toString());

                    bytes.release();

                    bytes = bytes2;
                }
            }
        }
    }

    @Test
    public void serializeCollections() {
        Bytes<?> bytes = new HexDumpBytes();
        MyCollections mc = new MyCollections();
        mc.words.add("Hello");
        mc.words.add("World");
        mc.scoreCountMap.put(1.3, 11L);
        mc.scoreCountMap.put(2.2, 22L);
        mc.policies.add(RetentionPolicy.RUNTIME);
        mc.policies.add(RetentionPolicy.CLASS);
        mc.numbers.add(1);
        mc.numbers.add(12);
        mc.numbers.add(123);
        mc.writeMarshallable(bytes);

        MyCollections mc2 = new MyCollections();
        mc2.readMarshallable(bytes);
        assertEquals(mc.words, mc2.words);
        assertEquals(mc.scoreCountMap, mc2.scoreCountMap);
        assertEquals(mc.policies, mc2.policies);
        assertEquals(mc.numbers, mc2.numbers);

        assertEquals(
                "   02 05 48 65 6c 6c 6f 05 57 6f 72 6c 64          # words\n" +
                        "   02 cd cc cc cc cc cc f4 3f 0b 00 00 00 00 00 00 # scoreCountMap\n" +
                        "   00 9a 99 99 99 99 99 01 40 16 00 00 00 00 00 00\n" +
                        "   00 02 07 52 55 4e 54 49 4d 45 05 43 4c 41 53 53 # policies\n" +
                        "   03 01 00 00 00 0c 00 00 00 7b 00 00 00          # numbers\n", bytes.toHexString());
        bytes.release();
    }

    @Test
    public void nested() {
        Bytes<?> bytes = new HexDumpBytes();
        BM1 bm1 = new BM1();
        bm1.num = 5;
        bm1.bm2.text = "hello";
        bm1.bm3.value = 1234567890L;

        bm1.writeMarshallable(bytes);

        BM1 bm1b = new BM1();
        bm1b.readMarshallable(bytes);
        assertEquals(bm1b.bm2.text, bm1.bm2.text);
        assertEquals(bm1b.bm3.value, bm1.bm3.value);

        assertEquals(
                "   05 00 00 00                                     # num\n" +
                        "                                                # bm2\n" +
                        "      05 68 65 6c 6c 6f                               # text\n" +
                        "                                                # bm3\n" +
                        "      d2 02 96 49 00 00 00 00                         # value\n", bytes.toHexString());
        assertEquals("# net.openhft.chronicle.bytes.BytesMarshallableTest$BM1\n" +
                "   05 00 00 00                                     # num\n" +
                "                                                # bm2\n" +
                "      05 68 65 6c 6c 6f                               # text\n" +
                "                                                # bm3\n" +
                "      d2 02 96 49 00 00 00 00                         # value\n", bm1.toString());
        bytes.release();
    }

    static class MyCollections implements BytesMarshallable {
        List<String> words = new ArrayList<>();
        Map<Double, Long> scoreCountMap = new LinkedHashMap<>();
        List<RetentionPolicy> policies = new ArrayList<>();
        List<Integer> numbers = new ArrayList<>();
    }

    static class BM1 implements BytesMarshallable {
        int num;
        BM2 bm2 = new BM2();
        BM3 bm3 = new BM3();

        @Override
        public String toString() {
            return $toString();
        }
    }

    static class BM2 implements BytesMarshallable {
        String text;
    }

    static class BM3 implements BytesMarshallable {
        long value;
    }
}

