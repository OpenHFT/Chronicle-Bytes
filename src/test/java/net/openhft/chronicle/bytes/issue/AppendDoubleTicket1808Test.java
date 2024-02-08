package net.openhft.chronicle.bytes.issue;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.bytes.BytesTestCommon;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class AppendDoubleTicket1808Test extends BytesTestCommon {
    @Test
    public void appendDoubleRounded08() {
        Bytes bytes = Bytes.allocateElasticOnHeap(64);
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
        Bytes bytes = Bytes.allocateElasticOnHeap(64);
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
        Bytes bytes = Bytes.allocateElasticOnHeap(64);
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
        Bytes bytes = Bytes.allocateElasticOnHeap(64);
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
        Bytes bytes = Bytes.allocateElasticOnHeap(64);
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
        Bytes bytes = Bytes.allocateElasticOnHeap(64);
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
        Bytes bytes = Bytes.allocateElasticOnHeap(64);
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
        Bytes bytes = Bytes.allocateElasticOnHeap(64);
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
        Bytes bytes = Bytes.allocateElasticOnHeap(64);
        for (double d : new double[]{0.14, 0.5}) {
            bytes.append(d, 16);
            String s = String.format("%.16f", d);
            assertEquals(s, bytes.toString());
            bytes.clear();
        }
        bytes.releaseLast();
    }
}
