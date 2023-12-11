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
package net.openhft.chronicle.bytes.readme;

import net.openhft.chronicle.bytes.BytesTestCommon;
import net.openhft.chronicle.bytes.HexDumpBytes;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class StopBitTest extends BytesTestCommon {

    @Test
    public void testString() {
        final HexDumpBytes bytes = new HexDumpBytes();
        try {
            for (long i : new long[]{
                    0, -1,
                    127, -127,
                    128, -128,
                    1 << 14, 1 << 21,
                    1 << 28, 1L << 35,
                    1L << 42, 1L << 49,
                    1L << 56, Long.MAX_VALUE,
                    Long.MIN_VALUE}) {
                bytes.writeHexDumpDescription(i + "L").writeStopBit(i);
            }

            for (double d : new double[]{
                    0.0,
                    -0.0,
                    1.0,
                    1.0625,
                    -128,
                    -Double.MIN_NORMAL,
                    Double.NEGATIVE_INFINITY,
                    Double.NaN,
                    Double.POSITIVE_INFINITY}) {
                bytes.writeHexDumpDescription(d + "").writeStopBit(d);
            }

            final String actual = bytes.toHexString();

            final String expected =
                    "00                                              # 0L\n" +
                            "80 00                                           # -1L\n" +
                            "7f                                              # 127L\n" +
                            "fe 00                                           # -127L\n" +
                            "80 01                                           # 128L\n" +
                            "ff 00                                           # -128L\n" +
                            "80 80 01                                        # 16384L\n" +
                            "80 80 80 01                                     # 2097152L\n" +
                            "80 80 80 80 01                                  # 268435456L\n" +
                            "80 80 80 80 80 01                               # 34359738368L\n" +
                            "80 80 80 80 80 80 01                            # 4398046511104L\n" +
                            "80 80 80 80 80 80 80 01                         # 562949953421312L\n" +
                            "80 80 80 80 80 80 80 80 01                      # 72057594037927936L\n" +
                            "ff ff ff ff ff ff ff ff 7f                      # 9223372036854775807L\n" +
                            "ff ff ff ff ff ff ff ff ff 00                   # -9223372036854775808L\n" +
                            "00                                              # 0.0\n" +
                            "40                                              # -0.0\n" +
                            "9f 7c                                           # 1.0\n" +
                            "9f fc 20                                        # 1.0625\n" +
                            "e0 18                                           # -128.0\n" +
                            "c0 04                                           # -2.2250738585072014E-308\n" +
                            "ff 7c                                           # -Infinity\n" +
                            "bf 7e                                           # NaN\n" +
                            "bf 7c                                           # Infinity\n";

            assertEquals(expected, actual);

            // System.out.println(actual);

        } finally {
            bytes.releaseLast();
        }
    }
}
