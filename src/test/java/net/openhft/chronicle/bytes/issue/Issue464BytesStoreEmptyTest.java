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

import net.openhft.chronicle.bytes.BytesStore;
import net.openhft.chronicle.bytes.BytesTestCommon;
import org.junit.Test;

import java.util.function.Supplier;

import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;

public class Issue464BytesStoreEmptyTest extends BytesTestCommon {
    @Test
    public void emptyShouldNotAllocate() {
        doTest(BytesStore::empty);
    }

    @Test
    public void nullByteArrayShouldNotAllocate() {
        doTest(() -> BytesStore.wrap((byte[]) null));
    }

    @Test
    public void allocateEmptyStringShouldNotAllocate() {
        doTest(() -> BytesStore.from(""));
    }

    @Test
    public void emptyBytesStoreShouldNotAllocate() {
        doTest(() -> BytesStore.from(BytesStore.empty()));
    }

    @Test
    public void emptyStringBuilderShouldNotAllocate() {
        doTest(() -> BytesStore.from(new StringBuilder()));
    }

    @Test
    public void emptyNativeShouldNotAllocate() {
        doTest(() -> BytesStore.nativeStoreWithFixedCapacity(0));
    }

    @Test(expected = NullPointerException.class)
    public void nullNativeStoreFromShouldNotAllocate() {
        doTest(() -> BytesStore.nativeStoreFrom(null));
    }

    @Test
    public void emptyNativeStoreFromShouldNotAllocate() {
        doTest(() -> BytesStore.nativeStoreFrom(new byte[0]));
    }

    @Test
    public void emptyCopyFromShouldNotAllocate() {
        doTest(() -> BytesStore.empty().copy());
    }

    @Test
    public void emptyByteArrayShouldHaveDifferentUnderlying() {
        BytesStore a = BytesStore.wrap(new byte[0]);
        BytesStore b = BytesStore.wrap(new byte[0]);
        assertNotSame(a, b);
        assertNotSame(a.underlyingObject(), b.underlyingObject());
    }

    private void doTest(Supplier<BytesStore> supplier) {
        assertSame(supplier.get(), supplier.get());
    }
}
