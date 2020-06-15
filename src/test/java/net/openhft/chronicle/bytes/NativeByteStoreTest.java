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

import net.openhft.chronicle.core.io.AbstractReferenceCounted;
import org.jetbrains.annotations.NotNull;
import org.junit.After;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

public class NativeByteStoreTest extends BytesTestCommon {

    @After
    public void checkRegisteredBytes() {
        AbstractReferenceCounted.assertReferencesReleased();
    }

    @SuppressWarnings("rawtypes")
    @Test
    public void testEquals() {
        @NotNull NativeBytesStore hbs = NativeBytesStore.from("Hello".getBytes());
        @NotNull NativeBytesStore hbs2 = NativeBytesStore.from("Hello".getBytes());
        @NotNull NativeBytesStore hbs3 = NativeBytesStore.from("He!!o".getBytes());
        @NotNull NativeBytesStore hbs4 = NativeBytesStore.from("Hi".getBytes());
        assertEquals(hbs, hbs2);
        assertEquals(hbs2, hbs);
        assertNotEquals(hbs, hbs3);
        assertNotEquals(hbs3, hbs);
        assertNotEquals(hbs, hbs4);
        assertNotEquals(hbs4, hbs);
    }
}
