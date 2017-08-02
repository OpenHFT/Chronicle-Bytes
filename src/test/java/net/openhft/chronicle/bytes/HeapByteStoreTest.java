/*
 * Copyright 2016 higherfrequencytrading.com
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
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

/*
 * Created by peter on 20/12/16.
 */
public class HeapByteStoreTest {
    @Test
    public void testEquals() {
        @NotNull HeapBytesStore hbs = HeapBytesStore.wrap("Hello".getBytes());
        @NotNull HeapBytesStore hbs2 = HeapBytesStore.wrap("Hello".getBytes());
        @NotNull HeapBytesStore hbs3 = HeapBytesStore.wrap("He!!o".getBytes());
        @NotNull HeapBytesStore hbs4 = HeapBytesStore.wrap("Hi".getBytes());
        assertEquals(hbs, hbs2);
        assertEquals(hbs2, hbs);
        assertNotEquals(hbs, hbs3);
        assertNotEquals(hbs3, hbs);
        assertNotEquals(hbs, hbs4);
        assertNotEquals(hbs4, hbs);
    }
}
