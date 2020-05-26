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

package net.openhft.chronicle.bytes;

import org.jetbrains.annotations.NotNull;
import org.junit.After;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class PointerBytesStoreTest {

    @After
    public void checkRegisteredBytes() {
        BytesUtil.checkRegisteredBytes();
    }

    @Test
    public void testWrap() throws IllegalArgumentException {
        @NotNull NativeBytesStore<Void> nbs = NativeBytesStore.nativeStore(10000);

        @NotNull PointerBytesStore pbs = BytesStore.nativePointer();
        pbs.set(nbs.addressForRead(nbs.start()), nbs.realCapacity());

        long nanoTime = System.nanoTime();
        pbs.writeLong(0L, nanoTime);

        assertEquals(nanoTime, nbs.readLong(0L));
    }
}