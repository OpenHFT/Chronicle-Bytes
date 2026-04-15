/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes;

import net.openhft.chronicle.bytes.render.GeneralDecimaliser;
import net.openhft.chronicle.core.Jvm;
import net.openhft.chronicle.core.io.IORuntimeException;
import net.openhft.chronicle.core.util.ObjectUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Collection;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

public class ByteStringAppenderTest extends BytesTestCommon {
    private Bytes<?> bytes;

    public void initByteStringAppenderTest(String name, boolean direct) {
        bytes = direct ? Bytes.allocateElasticDirect() : Bytes.elasticByteBuffer();
    }

    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
//                {"heap", false},
                {"native", true}
        });
    }

    @AfterEach
    @Override
    public void afterChecks() {
        bytes.releaseLast();
        super.afterChecks();
    }

    @MethodSource("data")
    @ParameterizedTest(name = "{0}")
    public void testConvertTo(String name, boolean direct) {
        initByteStringAppenderTest(name, direct);
        Bytes<?> hello = Bytes.from("hello");
        Bytes<?> hello1 = ObjectUtils.convertTo(Bytes.class, "hello");
        assertTrue(hello.contentEquals(hello1));
        VanillaBytes<Void> bytes = Bytes.allocateElasticDirect(2);
        Bytes<?> one = ObjectUtils.convertTo(Bytes.class, 1);
        assertTrue(bytes.append(1).contentEquals(one));
        one.releaseLast();
        hello1.releaseLast();
        hello.releaseLast();
        bytes.releaseLast();
    }

    @MethodSource("data")
    @ParameterizedTest(name = "{0}")
    public void testAppendInt(String name, boolean direct)
            throws IORuntimeException {
        initByteStringAppenderTest(name, direct);
        for (int expected = 1; expected != 0; expected *= 2) {
            bytes.append(expected);
            bytes.append(",");
            bytes.append(-expected);
            bytes.append(",");

            assertEquals(expected, (int) bytes.parseLong());
            assertEquals(-expected, (int) bytes.parseLong());
        }
    }

    @MethodSource("data")
    @ParameterizedTest(name = "{0}")
    public void testAppend(String name, boolean direct)
            throws IORuntimeException {
        initByteStringAppenderTest(name, direct);
        for (long expected = 1; expected != 0; expected *= 2) {
            bytes.clear();
            bytes.append(expected);
            bytes.append(",");
            bytes.append(-expected);
            bytes.append(",");
//            System.out.println(bytes);
            assertEquals(expected, bytes.parseLong());
            assertEquals(-expected, bytes.parseLong());
        }
    }

    @MethodSource("data")
    @ParameterizedTest(name = "{0}")
    public void testAppendWithOffset(String name, boolean direct) {
        initByteStringAppenderTest(name, direct);
        bytes.readLimit(20);
        bytes.writeLimit(20);
        for (long expected : new long[]{123456, 12345, 1234, 123, 12, 1, 0}) {
            bytes.append(10, expected, 6);
            assertEquals(expected, bytes.parseLong(10));
        }
    }

    @MethodSource("data")
    @ParameterizedTest(name = "{0}")
    public void testAppendWithOffsetNeg(String name, boolean direct) {
        initByteStringAppenderTest(name, direct);
        bytes.readLimit(20);
        bytes.writeLimit(20);
        for (long expected : new long[]{-123456, 12345, -1234, 123, -12, 1, 0}) {
            bytes.append(10, expected, 7);
            assertEquals(expected, bytes.parseLong(10));
        }
    }

    @MethodSource("data")
    @ParameterizedTest(name = "{0}")
    public void testAppendDouble(String name, boolean direct)
            throws IORuntimeException {
        initByteStringAppenderTest(name, direct);
        assumeFalse(GuardedNativeBytes.areNewGuarded());
        testAppendDouble0(-1.42278619425894E11);
/*
        @NotNull Random random = new Random(1);
        for (int i = 0; i < 100000; i++) {
            double d = Math.pow(1e32, random.nextDouble()) / 1e6;
            if (i % 3 == 0) d = -d;
            testAppendDouble0(d);
        }
*/
    }

    private void testAppendDouble0(double d)
            throws IORuntimeException {
        bytes.clear();
        bytes.append(d).append(' ');

        double d2 = bytes.parseDouble();
        assertEquals(d, d2, 0);

/* assumes self terminating.
        bytes.clear();
        bytes.appendDouble(d);
        bytes.flip();
        double d3 = bytes.parseDouble();
        Assert.assertEquals(d, d3, 0);
*/
    }

    @MethodSource("data")
    @ParameterizedTest(name = "{0}")
    public void testAppendLongDecimal(String name, boolean direct) {
        initByteStringAppenderTest(name, direct);
        assumeFalse(GuardedNativeBytes.areNewGuarded());
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
                "0.0001\n", bytes.toString());
    }

    @MethodSource("data")
    @ParameterizedTest(name = "{0}")
    public void testAppendDoublePrecision(String name, boolean direct) {
        initByteStringAppenderTest(name, direct);
        assumeFalse(GuardedNativeBytes.areNewGuarded());

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
                "64.550199\n", bytes.toString());
    }

    @MethodSource("data")
    @ParameterizedTest(name = "{0}")
    public void tens(String name, boolean direct) {
        initByteStringAppenderTest(name, direct);
        bytes.decimaliser(GeneralDecimaliser.GENERAL);
        for (int i = 0; i <= (int) Math.log10(Double.MAX_VALUE); i++) {
            bytes.clear();
            {
                double d = Math.pow(10, i);
                bytes.append(d).append(' ');
                String s = bytes.toString();
                double d2 = bytes.parseDouble();
                double ulp = i < 23 ? 0 : i < 235 ? Math.ulp(d) : Math.ulp(d) * 2;
                assertEquals(d, d2, ulp, s);
            }
            {
                double d = Math.pow(10, -i);
                bytes.append(d).append(' ');
                String s = bytes.toString();
                double d2 = bytes.parseDouble();
                assertEquals(d, d2, Jvm.isArm() ? 2E-27 : 2e-40, s);
            }
        }
    }

    @MethodSource("data")
    @ParameterizedTest(name = "{0}")
    public void testAppend8bit(String name, boolean direct) {
        initByteStringAppenderTest(name, direct);
        assumeFalse(Jvm.maxDirectMemory() == 0);

        BytesStore<?, ByteBuffer> bs = BytesStore.elasticByteBuffer(4, 16);
        bs.write(0, " -\n".getBytes());

        bytes.append8bit((CharSequence) bs, 1, 2);
        bytes.append8bit(bs, (long)0, 1);
        bytes.append8bit(bs, (long)2, 3);

        assertEquals("- \n", bytes.toString());
        bs.releaseLast();
    }
}
