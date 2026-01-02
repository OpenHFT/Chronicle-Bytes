/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes;

import net.openhft.chronicle.bytes.render.GeneralDecimaliser;
import net.openhft.chronicle.core.Jvm;
import net.openhft.chronicle.core.io.IORuntimeException;
import net.openhft.chronicle.core.util.ObjectUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

public class ByteStringAppenderTest extends BytesTestCommon {
    private Bytes<?> bytes;

    @BeforeEach
    void setUp() {
        bytes = Bytes.allocateElasticDirect();
    }

    @AfterEach
    void tearDown() {
        bytes.releaseLast();
        super.afterChecks();
    }

    @Test
    @DisplayName("convertTo produces bytes content for strings and numbers")
    public void testConvertTo() {
        Bytes<?> hello = Bytes.from("hello");
        Bytes<?> hello1 = ObjectUtils.convertTo(Bytes.class, "hello");
        assertTrue(hello.contentEquals(hello1),
                "string conversion should preserve content");
        VanillaBytes<Void> bytes = Bytes.allocateElasticDirect(2);
        Bytes<?> one = ObjectUtils.convertTo(Bytes.class, 1);
        assertTrue(bytes.append(1).contentEquals(one),
                "numeric conversion should preserve content");
        one.releaseLast();
        hello1.releaseLast();
        hello.releaseLast();
        bytes.releaseLast();
    }

    @Test
    @DisplayName("append int values round-trip through parseLong")
    public void testAppendInt()
            throws IORuntimeException {
        for (int expected = 1; expected != 0; expected *= 2) {
            bytes.append(expected);
            bytes.append(",");
            bytes.append(-expected);
            bytes.append(",");

            assertEquals(expected, (int) bytes.parseLong(),
                    "parseLong should read positive int for expected=" + expected);
            assertEquals(-expected, (int) bytes.parseLong(),
                    "parseLong should read negative int for expected=" + expected);
        }
    }

    @Test
    @DisplayName("append long values round-trip through parseLong")
    public void testAppend()
            throws IORuntimeException {
        for (long expected = 1; expected != 0; expected *= 2) {
            bytes.clear();
            bytes.append(expected);
            bytes.append(",");
            bytes.append(-expected);
            bytes.append(",");
            assertEquals(expected, bytes.parseLong(),
                    "parseLong should read positive long for expected=" + expected);
            assertEquals(-expected, bytes.parseLong(),
                    "parseLong should read negative long for expected=" + expected);
        }
    }

    @Test
    @DisplayName("append long values with offsets round-trip")
    public void testAppendWithOffset() {
        bytes.readLimit(20);
        bytes.writeLimit(20);
        for (long expected : new long[]{123456, 12345, 1234, 123, 12, 1, 0}) {
            bytes.append(10, expected, 6);
            assertEquals(expected, bytes.parseLong(10),
                    "parseLong should read offset value for expected=" + expected);
        }
    }

    @Test
    @DisplayName("append negative values with offsets round-trip")
    public void testAppendWithOffsetNeg() {
        bytes.readLimit(20);
        bytes.writeLimit(20);
        for (long expected : new long[]{-123456, 12345, -1234, 123, -12, 1, 0}) {
            bytes.append(10, expected, 7);
            assertEquals(expected, bytes.parseLong(10),
                    "parseLong should read negative offset value for expected=" + expected);
        }
    }

    @Test
    @DisplayName("append double values round-trip through parseDouble")
    public void testAppendDouble()
            throws IORuntimeException {
        assumeFalse(GuardedNativeBytes.areNewGuarded(),
                "guarded bytes use a different layout for double append");
        testAppendDouble0(-1.42278619425894E11);
    }

    private void testAppendDouble0(double d)
            throws IORuntimeException {
        bytes.clear();
        bytes.append(d).append(' ');

        double d2 = bytes.parseDouble();
        assertEquals(d, d2, 0,
                "parseDouble should round-trip appended value " + d);
    }

    @Test
    @DisplayName("appendDecimal formats long values with varying precision")
    public void testAppendLongDecimal() {
        assumeFalse(GuardedNativeBytes.areNewGuarded(),
                "guarded bytes use a different layout for decimal append");
        bytes.appendDecimal(128, 0).append('\n');
        bytes.appendDecimal(128, 1).append('\n');
        bytes.appendDecimal(128, 2).append('\n');
        bytes.appendDecimal(128, 3).append('\n');
        bytes.appendDecimal(128, 4).append('\n');

        bytes.appendDecimal(0, 0).append('\n');
        bytes.appendDecimal(0, 1).append('\n');
        bytes.appendDecimal(0, 4).append('\n');
        bytes.appendDecimal(1, 0).append('\n');
        bytes.appendDecimal(1, 1).append('\n');
        bytes.appendDecimal(1, 2).append('\n');
        bytes.appendDecimal(1, 3).append('\n');
        bytes.appendDecimal(1, 4).append('\n');

        assertEquals("128\n" +
                "12.8\n" +
                "1.28\n" +
                "0.128\n" +
                "0.0128\n" +
                "0\n" +
                "0.0\n" +
                "0.0000\n" +
                "1\n" +
                "0.1\n" +
                "0.01\n" +
                "0.001\n" +
                "0.0001\n", bytes.toString(),
                "appendDecimal output should match expected formatting");
    }

    @Test
    @DisplayName("append formats doubles with requested precision")
    public void testAppendDoublePrecision() {
        assumeFalse(GuardedNativeBytes.areNewGuarded(),
                "guarded bytes use a different layout for precision append");

        bytes.append(1.28, 0).append('\n');
        bytes.append(-1.28, 1).append('\n');
        bytes.append(1.28, 2).append('\n');
        bytes.append(-1.28, 3).append('\n');
        bytes.append(1.28, 4).append('\n');

        bytes.append(0, 0).append('\n');
        bytes.append(-0, 1).append('\n');
        bytes.append(0, 4).append('\n');
        bytes.append(1, 0).append('\n');
        bytes.append(-1.11, 1).append('\n');
        bytes.append(0.111, 2).append('\n');
        bytes.append(1.1, 3).append('\n');
        bytes.append(-0.01111, 4).append('\n');

        bytes.append(64.5501985, 6).append('\n');

        assertEquals("1\n" +
                "-1.3\n" +
                "1.28\n" +
                "-1.280\n" +
                "1.2800\n" +
                "0\n" +
                "0.0\n" +
                "0.0000\n" +
                "1\n" +
                "-1.1\n" +
                "0.11\n" +
                "1.100\n" +
                "-0.0111\n" +
                "64.550199\n", bytes.toString(),
                "precision formatting should match expected output");
    }

    @Test
    @DisplayName("decimaliser handles powers of ten correctly")
    public void tens() {
        bytes.decimaliser(GeneralDecimaliser.GENERAL);
        for (int i = 0; i <= (int) Math.log10(Double.MAX_VALUE); i++) {
            bytes.clear();
            {
                double d = Math.pow(10, i);
                bytes.append(d).append(' ');
                String s = bytes.toString();
                double d2 = bytes.parseDouble();
                double ulp = i < 23 ? 0 : i < 235 ? Math.ulp(d) : Math.ulp(d) * 2;
                assertEquals(d, d2, ulp,
                        "positive power-of-ten should round-trip for i=" + i + ", text=" + s);
            }
            {
                double d = Math.pow(10, -i);
                bytes.append(d).append(' ');
                String s = bytes.toString();
                double d2 = bytes.parseDouble();
                assertEquals(d, d2, Jvm.isArm() ? 2E-27 : 2e-40,
                        "negative power-of-ten should round-trip for i=" + i + ", text=" + s);
            }
        }
    }

    @Test
    @DisplayName("append8bit copies selected slices of BytesStore")
    public void testAppend8bit() {
        assumeFalse(Jvm.maxDirectMemory() == 0,
                "append8bit requires direct memory availability");

        BytesStore<?, ByteBuffer> bs = BytesStore.elasticByteBuffer(4, 16);
        bs.write(0, " -\n".getBytes(StandardCharsets.ISO_8859_1));

        bytes.append8bit((CharSequence) bs, 1, 2);
        bytes.append8bit(bs, (long)0, 1);
        bytes.append8bit(bs, (long)2, 3);

        assertEquals("- \n", bytes.toString(),
                "append8bit should merge slices into expected text");
        bs.releaseLast();
    }
}
