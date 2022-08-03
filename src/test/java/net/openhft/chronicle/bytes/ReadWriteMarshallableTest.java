/*
 * Copyright (c) 2016-2022 chronicle.software
 *
 *       https://chronicle.software
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

import net.openhft.chronicle.core.io.IORuntimeException;
import org.junit.Test;

import java.nio.BufferUnderflowException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assume.assumeFalse;

@SuppressWarnings("rawtypes")
public class ReadWriteMarshallableTest extends BytesTestCommon {
    @Test
    public void test()
            throws BufferUnderflowException, IllegalStateException {
        // TODO Make guarded safe
        assumeFalse(NativeBytes.areNewGuarded());

        Bytes<?> bytes = Bytes.allocateElasticOnHeap(128);
        Bytes<?> hello_world = Bytes.from("Hello World");
        Bytes<?> bye = Bytes.from("Bye");
        RWOuter o = new RWOuter(
                new RWInner(hello_world),
                new RWInner(bye));

        bytes.writeMarshallableLength16(o);

        RWOuter o2 = bytes.readMarshallableLength16(RWOuter.class, null);
        assertEquals("Hello World", o2.i1.data.toString());
        assertEquals("Bye", o2.i2.data.toString());
        hello_world.releaseLast();
        bye.releaseLast();
    }

    static class RWOuter implements BytesMarshallable {
        RWInner i1, i2;

        public RWOuter(RWInner i1, RWInner i2) {
            this.i1 = i1;
            this.i2 = i2;
        }

        @Override
        public void readMarshallable(BytesIn<?> bytes)
                throws IORuntimeException, BufferUnderflowException {
            BytesIn<?> in = (BytesIn<?>) bytes;
            i1 = in.readMarshallableLength16(RWInner.class, i1);
            i2 = in.readMarshallableLength16(RWInner.class, i2);
        }

        @Override
        public void writeMarshallable(BytesOut<?> bytes) {
            bytes.writeMarshallableLength16(i1);
            bytes.writeMarshallableLength16(i2);
        }
    }

    static class RWInner implements BytesMarshallable {
        Bytes<?> data;

        public RWInner(Bytes<?> data) {
            this.data = data;
        }

        @Override
        public void readMarshallable(BytesIn<?> bytes)
                throws IORuntimeException {
            if (data == null) data = Bytes.allocateElasticOnHeap(64);
            data.clear().write((BytesStore) bytes);
        }

        @Override
        public void writeMarshallable(BytesOut<?> bytes) {
            bytes.write(data);
        }
    }
}
