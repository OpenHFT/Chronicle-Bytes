/*
 * Copyright (c) 2016-2022 chronicle.software
 *
 * https://chronicle.software
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
import net.openhft.chronicle.core.Mocker;
import org.junit.Test;

import java.io.StringWriter;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.ZonedDateTime;
import java.util.UUID;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assume.assumeFalse;

public class BytesMethodWriterBuilderTest extends BytesTestCommon {

    @Test
    public void testPrimitives() {
        assumeFalse(NativeBytes.areNewGuarded());
        final Bytes<?> bytes = new HexDumpBytes();
        try {
            final IBytesMethod m = bytes.bytesMethodWriter(IBytesMethod.class);

            final MyByteable mb1 = new MyByteable(false, (byte) 1, (short) 2, '3', 4, 5.5f, 6, 7.7);
            final MyByteable mb2 = new MyByteable(true, (byte) 11, (short) 22, 'T', 44, 5.555f, 66, 77.77);

            m.myByteable(mb1);
            m.myByteable(mb2);

            final ZonedDateTime zdt1 = ZonedDateTime.parse("2017-11-06T12:35:56.775Z[Europe/London]");
            final ZonedDateTime zdt2 = ZonedDateTime.parse("2016-10-05T01:34:56.775Z[Europe/London]");
            final UUID uuid1 = new UUID(0x123456789L, 0xABCDEF);
            final UUID uuid2 = new UUID(0x1111111111111111L, 0x2222222222222222L);
            final MyScalars ms1 = new MyScalars("Hello", BigInteger.ONE, BigDecimal.TEN, zdt1.toLocalDate(), zdt1.toLocalTime(), zdt1.toLocalDateTime(), zdt1, uuid1);
            final MyScalars ms2 = new MyScalars("World", BigInteger.ZERO, BigDecimal.ZERO, zdt2.toLocalDate(), zdt2.toLocalTime(), zdt2.toLocalDateTime(), zdt2, uuid2);
            final MyNested mn2 = new MyNested(mb2, ms2);

            m.myScalars(ms1);

            m.myNested(mn2);

            assertEquals("" +
                    "81 01                                           # myByteable\n" +
                    "   4e                                              # flag\n" +
                    "   01                                              # b\n" +
                    "   02 00                                           # s\n" +
                    "   33                                              # c\n" +
                    "   04 00 00 00                                     # i\n" +
                    "   00 00 b0 40                                     # f\n" +
                    "   06 00 00 00 00 00 00 00                         # l\n" +
                    "   cd cc cc cc cc cc 1e 40                         # d\n" +
                    "81 01                                           # myByteable\n" +
                    "   59                                              # flag\n" +
                    "   0b                                              # b\n" +
                    "   16 00                                           # s\n" +
                    "   54                                              # c\n" +
                    "   2c 00 00 00                                     # i\n" +
                    "   8f c2 b1 40                                     # f\n" +
                    "   42 00 00 00 00 00 00 00                         # l\n" +
                    "   e1 7a 14 ae 47 71 53 40                         # d\n" +
                    "82 01                                           # myScalars\n" +
                    "   05 48 65 6c 6c 6f                               # s\n" +
                    "   01 31                                           # bi\n" +
                    "   02 31 30                                        # bd\n" +
                    "   0a 32 30 31 37 2d 31 31 2d 30 36                # date\n" +
                    "   0c 31 32 3a 33 35 3a 35 36 2e 37 37 35          # time\n" +
                    "   17 32 30 31 37 2d 31 31 2d 30 36 54 31 32 3a 33 # dateTime\n" +
                    "   35 3a 35 36 2e 37 37 35 27 32 30 31 37 2d 31 31 # zonedDateTime\n" +
                    "   2d 30 36 54 31 32 3a 33 35 3a 35 36 2e 37 37 35\n" +
                    "   5a 5b 45 75 72 6f 70 65 2f 4c 6f 6e 64 6f 6e 5d # uuid\n" +
                    "   24 30 30 30 30 30 30 30 31 2d 32 33 34 35 2d 36\n" +
                    "   37 38 39 2d 30 30 30 30 2d 30 30 30 30 30 30 61\n" +
                    "   62 63 64 65 66\n" +
                    "83 01                                           # myNested\n" +
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
                    "      32 32 32 32 32 32 32 32 32 32\n", bytes.toHexString());

            final StringWriter out = new StringWriter();
            final MethodReader reader = bytes.bytesMethodReader(Mocker.logging(IBytesMethod.class, "* ", out));

            for (int i = 0; i < 4; i++) {
                assertTrue(reader.readOne());
            }
            assertFalse(reader.readOne());

            final String expected =
                    Jvm.isJava9Plus() ?
                            "* myByteable[MyByteable{flag=false, b=1, s=2, c=3, i=4, f=5.5, l=6, d=7.7}]\n" +
                                    "* myByteable[MyByteable{flag=true, b=11, s=22, c=T, i=44, f=5.555, l=66, d=77.77}]\n" +
                                    "* myScalars[MyScalars{s='Hello', bi=1, bd=10, date=2017-11-06, time=12:35:56.775, dateTime=2017-11-06T12:35:56.775, zonedDateTime=2017-11-06T12:35:56.775Z[Europe/London], uuid=00000001-2345-6789-0000-000000abcdef}]\n" +
                                    "* myNested[MyNested{byteable=MyByteable{flag=true, b=11, s=22, c=T, i=44, f=5.555, l=66, d=77.77}, scalars=MyScalars{s='World', bi=0, bd=0, date=2016-10-05, time=02:34:56.775, dateTime=2016-10-05T02:34:56.775, zonedDateTime=2016-10-05T02:34:56.775+01:00[Europe/London], uuid=11111111-1111-1111-2222-222222222222}}]\n" :
                            "* myByteable[MyByteable{flag=false, b=1, s=2, c=3, i=4, f=5.5, l=6, d=7.7}]\n" +
                                    "* myByteable[MyByteable{flag=true, b=11, s=22, c=T, i=44, f=5.555, l=66, d=77.77}]\n" +
                                    "* myScalars[MyScalars{s='Hello', bi=1, bd=10, date=2017-11-06, time=12:35:56.775, dateTime=2017-11-06T12:35:56.775, zonedDateTime=2017-11-06T12:35:56.775Z[Europe/London], uuid=00000001-2345-6789-0000-000000abcdef}]\n" +
                                    "* myNested[MyNested{byteable=MyByteable{flag=true, b=11, s=22, c=T, i=44, f=5.555, l=66, d=77.77}, scalars=MyScalars{s='World', bi=0, bd=0, date=2016-10-05, time=01:34:56.775, dateTime=2016-10-05T01:34:56.775, zonedDateTime=2016-10-05T01:34:56.775+01:00[Europe/London], uuid=11111111-1111-1111-2222-222222222222}}]\n";

/*        System.out.println(expected);
        System.out.println(out.toString().replaceAll("\n", ""));*/

            assertEquals(expected,
                    out.toString().replaceAll("\r", ""));
        } finally {
            bytes.releaseLast();
        }

    }
}