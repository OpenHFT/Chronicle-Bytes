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
package net.openhft.chronicle.bytes.ref;

import net.openhft.chronicle.bytes.BytesStore;
import net.openhft.chronicle.bytes.BytesTestCommon;
import net.openhft.chronicle.bytes.MappedBytesStore;
import net.openhft.chronicle.bytes.MappedFile;
import net.openhft.chronicle.core.io.IOTools;
import net.openhft.chronicle.core.io.ReferenceOwner;
import org.jetbrains.annotations.NotNull;
import org.junit.Test;

import java.io.File;
import java.io.IOException;

import static org.junit.Assert.*;

public class BinaryLongReferenceTest extends BytesTestCommon {
    @Test
    public void test() {
        @NotNull BytesStore nbs = BytesStore.nativeStoreWithFixedCapacity(32);
        try (@NotNull BinaryLongReference ref = new BinaryLongReference()) {
            ref.bytesStore(nbs, 16, 8);
            assertEquals(0, ref.getValue());
            ref.addAtomicValue(1);
            assertEquals(1, ref.getVolatileValue());
            ref.addValue(-2);
            assertEquals("value: -1", ref.toString());
            assertFalse(ref.compareAndSwapValue(0, 1));
            assertTrue(ref.compareAndSwapValue(-1, 2));
            assertEquals(8, ref.maxSize());
            assertEquals(16, ref.offset());
            assertEquals(nbs, ref.bytesStore());
            assertEquals(0L, nbs.readLong(0));
            assertEquals(0L, nbs.readLong(8));
            assertEquals(2L, nbs.readLong(16));
            assertEquals(0L, nbs.readLong(24));

            ref.setValue(10);
            assertEquals(10L, nbs.readLong(16));
            ref.setOrderedValue(20);
            Thread.yield();
            assertEquals(20L, nbs.readVolatileLong(16));
        }
        nbs.releaseLast();
    }

    @Test
    public void testCanAssignByteStoreWithExistingOffsetNotInRange() throws IOException {
        final File tempFile = IOTools.createTempFile("testCanAssignByteStoreWithExistingOffsetNotInRange");
        final ReferenceOwner referenceOwner = ReferenceOwner.temporary("test");
        try (final MappedFile mappedFile = MappedFile.mappedFile(tempFile, 4096)) {
            MappedBytesStore bytes = mappedFile.acquireByteStore(referenceOwner, 8192);
            try (final BinaryLongReference blr = new BinaryLongReference()) {
                blr.bytesStore(bytes, 8192, 8);
                blr.setValue(1234);
                assertEquals(1234, blr.getValue());
            } finally {
                bytes.release(referenceOwner);
            }
        }
    }
}
