/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * This class contains JUnit test methods for testing the behavior
 * of the GuardedNativeBytes class.
 * <p>
 * It aims to test various primitive data types and their conversions
 * using the GuardedNativeBytes class.
 */
@SuppressWarnings("deprecation")
public class GuardedNativeBytesTest {

    /**
     * Tests the reading and writing of various binary primitives.
     * <p>
     * This test method performs the following steps:
     * <ul>
     *   <li>Writes different types of binary data into a GuardedNativeBytes object.</li>
     *   <li>Checks the generated hexadecimal string against an expected value.</li>
     *   <li>Reads the binary data back and checks that it matches the original input.</li>
     * </ul>
         */
    @Test
    public void testBinaryPrimitive() {
        final GuardedNativeBytes<?> bytes = new GuardedNativeBytes<>(new HexDumpBytes(), 256);
        try {
            PrimitiveTestSupport.writeBinaryPrimitivePayload(bytes);
            bytes.writeHexDumpDescription("Utf8").writeUtf8("Hello");

            final String expected = "a4 59                                           # flag\n" +
                    "a4 01                                           # s8\n" +
                    "a4 02                                           # u8\n" +
                    "a5 03 00                                        # s16\n" +
                    "a5 04 00                                        # u16\n" +
                    "ae 35                                           # ch\n" +
                    "a5 56 46 a4 9a                                  # s24\n" +
                    "a5 2a 50 a4 fe                                  # u24\n" +
                    "a6 06 00 00 00                                  # s32\n" +
                    "a6 07 00 00 00                                  # u32\n" +
                    "a7 08 00 00 00 00 00 00 00                      # s64\n" +
                    "90 00 00 10 41                                  # f32\n" +
                    "91 00 00 00 00 00 00 24 40                      # f64\n" +
                    "ae 05 48 65 6c 6c 6f                            # Utf8\n";

            final String actual = bytes.toHexString();

            assertEquals(expected, actual, "GuardedNativeBytes should generate expected hex dump format with descriptions for binary primitives");

            PrimitiveTestSupport.assertBinaryPrimitiveValues(bytes, true, "Hello");
        } finally {
            bytes.releaseLast();
        }
    }
}
