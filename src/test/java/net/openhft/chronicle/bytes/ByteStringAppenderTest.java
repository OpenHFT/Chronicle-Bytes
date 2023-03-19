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
import net.openhft.chronicle.core.io.IORuntimeException;
import net.openhft.chronicle.core.util.ObjectUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collection;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeFalse;

@RunWith(Parameterized.class)
public class ByteStringAppenderTest extends BytesTestCommon {
    private final Bytes<?> bytes;

    public ByteStringAppenderTest(String name, boolean direct) {
        bytes = direct ? Bytes.allocateElasticDirect() : Bytes.elasticByteBuffer();
    }

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
//                {"heap", false},
                {"native", true}
        });
    }

    @Override
    public void afterChecks() {
        bytes.releaseLast();
        super.afterChecks();
    }

    @Test
    public void testConvertTo() {
        Bytes<?> hello = Bytes.from("hello");
        Bytes<?> hello1 = ObjectUtils.convertTo(Bytes.class, "hello");
        assertTrue(hello.contentEquals(hello1));
        VanillaBytes<Void> bytes = Bytes.allocateDirect(2);
        Bytes<?> one = ObjectUtils.convertTo(Bytes.class, 1);
        assertTrue(bytes.append(1).contentEquals(one));
        one.releaseLast();
        hello1.releaseLast();
        hello.releaseLast();
        bytes.releaseLast();
    }

    @Test
    public void testAppendInt()
            throws IORuntimeException {
        for (int expected = 1; expected != 0; expected *= 2) {
            bytes.append(expected);
            bytes.append(",");
            bytes.append(-expected);
            bytes.append(",");

            assertEquals(expected, (int) bytes.parseLong());
            assertEquals(-expected, (int) bytes.parseLong());
        }
    }

    @Test
    public void testAppend()
            throws IORuntimeException {
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

    @Test
    public void testAppendWithOffset() {
        bytes.readLimit(20);
        bytes.writeLimit(20);
        for (long expected : new long[]{123456, 12345, 1234, 123, 12, 1, 0}) {
            bytes.append(10, expected, 6);
            assertEquals(expected, bytes.parseLong(10));
        }
    }

    @Test
    public void testAppendWithOffsetNeg() {
        bytes.readLimit(20);
        bytes.writeLimit(20);
        for (long expected : new long[]{-123456, 12345, -1234, 123, -12, 1, 0}) {
            bytes.append(10, expected, 7);
            assertEquals(expected, bytes.parseLong(10));
        }
    }

    @Test
    public void testAppendDouble()
            throws IORuntimeException {
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

    @Test
    public void testAppendLongDecimal() {
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

    @Test
    public void testAppendDoublePrecision() {
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

    @Test
    public void tens() {
        for (int i = 0; i <= (int) Math.log10(Double.MAX_VALUE); i++) {
            bytes.clear();
            {
                double d = Math.pow(10, i);
                bytes.append(d).append(' ');
                String s = bytes.toString();
                double d2 = bytes.parseDouble();
                double ulp = i < 31 ? 0 : i < 235 ? Math.ulp(d) : Math.ulp(d) * 2;
                assertEquals(s, d, d2, ulp);
            }
            {
                double d = Math.pow(10, -i);
                bytes.append(d).append(' ');
                String s = bytes.toString();
                double d2 = bytes.parseDouble();
                assertEquals(s, d, d2, Jvm.isArm() ? 2e-39 : 2e-40);
            }
        }
    }

    @Test
    public void testAppend8bit() {
        BytesStore bs = BytesStore.elasticByteBuffer(4, 16);
        bs.write(0, " -\n".getBytes());

        bytes.append8bit((CharSequence) bs, 1, 2);
        bytes.append8bit(bs, (long)0, 1);
        bytes.append8bit(bs, (long)2, 3);

        assertEquals("- \n", bytes.toString());
    }
}