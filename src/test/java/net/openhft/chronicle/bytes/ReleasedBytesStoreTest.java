/*
 * Copyright (c) 2016-2022 chronicle.software
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

import net.openhft.chronicle.bytes.internal.NativeBytesStore;
import org.junit.Test;

import static org.junit.Assert.*;

public class ReleasedBytesStoreTest extends BytesTestCommon {

    @Test
    public void release() {
        Bytes<?> bytes = Bytes.allocateElasticDirect();
        assertNull(bytes.bytesStore().underlyingObject());
        bytes.writeLong(0, 0);
        assertEquals(NativeBytesStore.class, bytes.bytesStore().getClass());
        bytes.releaseLast();
        assertEquals(0, bytes.bytesStore().refCount());
        try {
            bytes.writeLong(0, 0);
            fail();
        } catch (NullPointerException e) {
            // expected.
        }
    }
}