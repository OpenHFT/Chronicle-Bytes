/*
 * Copyright 2016-2020 Chronicle Software
 *
 * https://chronicle.software
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
package net.openhft.chronicle.bytes.ref;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.bytes.BytesMarshallable;
import net.openhft.chronicle.bytes.BytesTestCommon;
import net.openhft.chronicle.bytes.NativeBytes;
import net.openhft.chronicle.core.threads.ThreadDump;
import org.jetbrains.annotations.NotNull;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assume.assumeFalse;

public class BinaryLongArrayReferenceTest extends BytesTestCommon {

    private ThreadDump threadDump;

    @Before
    public void threadDump() {
        threadDump = new ThreadDump();
    }

    @After
    public void checkThreadDump() {
        threadDump.assertNoNewThreads();
    }

    @SuppressWarnings("rawtypes")
    @Test
    public void getSetValues() {
        int length = 128 * 8 + 2 * 8;
        Bytes bytes = Bytes.allocateDirect(length);
        BinaryLongArrayReference.write(bytes, 128);

        try (@NotNull BinaryLongArrayReference array = new BinaryLongArrayReference()) {
            array.bytesStore(bytes, 0, length);

            assertEquals(128, array.getCapacity());
            for (int i = 0; i < 128; i++)
                array.setValueAt(i, i + 1);

            for (int i = 0; i < 128; i++)
                assertEquals(i + 1, array.getValueAt(i));
            bytes.release();
        }
    }

    @SuppressWarnings("rawtypes")
    @Test
    public void marshallable() {
        assumeFalse(NativeBytes.areNewGuarded());
        Bytes bytes = Bytes.allocateElasticDirect(256);
        LongArrays la = new LongArrays(4, 8);
        la.writeMarshallable(bytes);
        System.out.println(bytes.toHexString());

        LongArrays la2 = new LongArrays(0, 0);
        la2.readMarshallable(bytes);
        assertEquals(4, la2.first.getCapacity());
        assertEquals(8, la2.second.getCapacity());
        la.closeAll();
        la2.closeAll();
        bytes.release();
    }

    static class LongArrays implements BytesMarshallable {
        BinaryLongArrayReference first = new BinaryLongArrayReference();
        BinaryLongArrayReference second = new BinaryLongArrayReference();

        public LongArrays(int firstLength, int secondLength) {
            first.capacity(firstLength);
            second.capacity(secondLength);
        }

        public void closeAll() {
            first.close();
            second.close();
        }
    }
}