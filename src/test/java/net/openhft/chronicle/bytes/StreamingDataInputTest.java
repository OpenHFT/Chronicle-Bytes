package net.openhft.chronicle.bytes;

import net.openhft.chronicle.core.Jvm;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Objects;

import static org.junit.Assert.assertEquals;
import static org.junit.Assume.assumeFalse;

@RunWith(Parameterized.class)
public class StreamingDataInputTest extends BytesTestCommon {

    interface BytesFactory {
        Bytes<?> createBuffer();
    }

    private enum BytesType implements BytesFactory {
        DIRECT {
            @Override
            public Bytes<?> createBuffer() {
                return Bytes.allocateElasticDirect();
            }
        },
        ON_HEAP {
            @Override
            public Bytes<?> createBuffer() {
                return Bytes.allocateElasticOnHeap();
            }
        }
    }

    private BytesType bytesType;

    @Parameterized.Parameters(name="bytesType={0}")
    public static Object[] params() {
        return Arrays.stream(BytesType.values()).toArray();
    }

    public StreamingDataInputTest(BytesType bytesType) {
        this.bytesType = bytesType;
    }

    @Test
    public void read() {
        Bytes<?> b = bytesType.createBuffer();
        b.append("0123456789");
        byte[] byteArr = "ABCDEFGHIJKLMNOP".getBytes();
        b.read(byteArr, 2, 6);
        assertEquals("AB012345IJKLMNOP", new String(byteArr, StandardCharsets.ISO_8859_1));
        assertEquals('6', b.readByte());
        b.releaseLast();
    }

    @Test
    public void roundTripWorksOnHeap() {
        assumeFalse(Jvm.isAzulZing());
        Bytes<?> b = bytesType.createBuffer();
        TestObject source = new TestObject(123L, 123, false);
        b.unsafeWriteObject(source, 13);
        TestObject dest = new TestObject();
        b.unsafeReadObject(dest, 13);
        assertEquals(source, dest);
    }

    static class TestObject {
        long l1;
        long i1;
        boolean b1;

        public TestObject() {
        }

        public TestObject(long l1, int i1, boolean b1) {
            this.l1 = l1;
            this.i1 = i1;
            this.b1 = b1;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            TestObject that = (TestObject) o;
            return l1 == that.l1 && i1 == that.i1 && b1 == that.b1;
        }

        @Override
        public int hashCode() {
            return Objects.hash(l1, i1, b1);
        }

        @Override
        public String toString() {
            return "TestObject{" +
                    "l1=" + l1 +
                    ", i1=" + i1 +
                    ", b1=" + b1 +
                    '}';
        }
    }
}