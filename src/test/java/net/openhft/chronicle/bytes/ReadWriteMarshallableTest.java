package net.openhft.chronicle.bytes;

import net.openhft.chronicle.core.io.IORuntimeException;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ReadWriteMarshallableTest {
    @Test
    public void test() {
        Bytes<?> bytes = Bytes.elasticHeapByteBuffer(128);
        RWOuter o = new RWOuter(
                new RWInner(Bytes.from("Hello World")),
                new RWInner(Bytes.from("Bye")));

        bytes.writeMarshallableLength16(o);

        RWOuter o2 = bytes.readMarshallableLength16(RWOuter.class, null);
        assertEquals("Hello World", o2.i1.data.toString());
        assertEquals("Bye", o2.i2.data.toString());
    }

    static class RWOuter implements BytesMarshallable {
        RWInner i1, i2;

        public RWOuter(RWInner i1, RWInner i2) {
            this.i1 = i1;
            this.i2 = i2;
        }

        @Override
        public void readMarshallable(BytesIn bytes) throws IORuntimeException {
            BytesIn<?> in = (BytesIn<?>) bytes;
            i1 = in.readMarshallableLength16(RWInner.class, i1);
            i2 = in.readMarshallableLength16(RWInner.class, i2);
        }

        @Override
        public void writeMarshallable(BytesOut bytes) {
            bytes.writeMarshallableLength16(i1);
            bytes.writeMarshallableLength16(i2);
        }
    }

    static class RWInner implements BytesMarshallable {
        Bytes data;

        public RWInner(Bytes data) {
            this.data = data;
        }

        @Override
        public void readMarshallable(BytesIn bytes) throws IORuntimeException {
            if (data == null) data = Bytes.elasticHeapByteBuffer(64);
            data.clear().write((BytesStore) bytes);
        }

        @Override
        public void writeMarshallable(BytesOut bytes) {
            bytes.write(data);
        }
    }
}
