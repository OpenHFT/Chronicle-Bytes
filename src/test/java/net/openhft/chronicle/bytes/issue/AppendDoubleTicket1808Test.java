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
package net.openhft.chronicle.bytes.issue;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.bytes.BytesTestCommon;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class AppendDoubleTicket1808Test extends BytesTestCommon {
    @Test
    public void appendDoubleRounded08() {
        Bytes<?> bytes = Bytes.allocateElasticOnHeap(64);
        for (double d : new double[]{2.01, 16777216.1, 67108864}) {
            bytes.append(d, 8);
            String s = String.format("%.8f", d);
            assertEquals(s, bytes.toString());
            bytes.clear();
        }
        bytes.releaseLast();
    }

    @Test
    public void appendDoubleRounded09() {
        Bytes<byte[]> bytes = Bytes.allocateElasticOnHeap(64);
        for (double d : new double[]{2.01, 2097152.2, 8388608}) {
            bytes.append(d, 9);
            String s = String.format("%.9f", d);
            assertEquals(s, bytes.toString());
            bytes.clear();
        }
        bytes.releaseLast();
    }

    @Test
    public void appendDoubleRounded10() {
        Bytes<byte[]> bytes = Bytes.allocateElasticOnHeap(64);
        for (double d : new double[]{0.41, 131072.14, 524288}) {
            bytes.append(d, 10);
            String s = String.format("%.10f", d);
            assertEquals(s, bytes.toString());
            bytes.clear();
        }
        bytes.releaseLast();
    }

    @Test
    public void appendDoubleRounded11() {
        Bytes<byte[]> bytes = Bytes.allocateElasticOnHeap(64);
        for (double d : new double[]{0.29, 16384.06, 327638.3}) {
            bytes.append(d, 11);
            String s = String.format("%.11f", d);
            assertEquals(s, bytes.toString());
            bytes.clear();
        }
        bytes.releaseLast();
    }

    @Test
    public void appendDoubleRounded12() {
        Bytes<byte[]> bytes = Bytes.allocateElasticOnHeap(64);
        for (double d : new double[]{2.01, 2048.01, 4096.1}) {
            bytes.append(d, 12);
            String s = String.format("%.12f", d);
            assertEquals(s, bytes.toString());
            bytes.clear();
        }
        bytes.releaseLast();
    }

    @Test
    public void appendDoubleRounded13() {
        Bytes<byte[]> bytes = Bytes.allocateElasticOnHeap(64);
        for (double d : new double[]{0.41, 128.08, 512.0}) {
            bytes.append(d, 13);
            String s = String.format("%.13f", d);
            assertEquals(s, bytes.toString());
            bytes.clear();
        }
        bytes.releaseLast();
    }

    @Test
    public void appendDoubleRounded14() {
        Bytes<byte[]> bytes = Bytes.allocateElasticOnHeap(64);
        for (double d : new double[]{0.29, 16.01, 32.2}) {
            bytes.append(d, 14);
            String s = String.format("%.14f", d);
            assertEquals(s, bytes.toString());
            bytes.clear();
        }
        bytes.releaseLast();
    }

    @Test
    public void appendDoubleRounded15() {
        Bytes<byte[]> bytes = Bytes.allocateElasticOnHeap(64);
        for (double d : new double[]{2.16, 4.4}) {
            bytes.append(d, 15);
            String s = String.format("%.15f", d);
            assertEquals(s, bytes.toString());
            bytes.clear();
        }
        bytes.releaseLast();
    }

    @Test
    public void appendDoubleRounded16() {
        Bytes<byte[]> bytes = Bytes.allocateElasticOnHeap(64);
        for (double d : new double[]{0.00014, 0.14, 0.5}) {
            bytes.append(d, 16);
            String s = String.format("%.16f", d);
            assertEquals(s, bytes.toString());
            bytes.clear();
        }
        bytes.releaseLast();
    }

    @Test
    public void appendDoubleRounded17() {
        Bytes<byte[]> bytes = Bytes.allocateElasticOnHeap(64);
        for (double d : new double[]{0.00014, 0.5, 2.16, 4.4, 32.2}) {
            bytes.append(d, 17);
            String s = String.format("%.17f", d);
            assertEquals(s, bytes.toString());
            bytes.clear();
        }
        bytes.releaseLast();
    }

    @Test
    public void appendDoubleRounded18() {
        Bytes<byte[]> bytes = Bytes.allocateElasticOnHeap(64);
        for (double d : new double[]{0.000014, 0.5, 2.16, 4.4, 32.2}) {
            bytes.append(d, 18);
            String s = String.format("%.18f", d);
            assertEquals(s, bytes.toString());
            bytes.clear();
        }
        bytes.releaseLast();
    }

    @Test
    public void appendDoubleRounded19Plus() {
        Bytes<byte[]> bytes = Bytes.allocateElasticOnHeap(64);
        for (double d : new double[]{0.14, 0.5, 2.16, 4.4, 32.2}) {
            bytes.append(d, 19);
            String s = Double.toString(d);
            assertEquals(s, bytes.toString());
            bytes.clear();
        }
        bytes.releaseLast();
    }
}
