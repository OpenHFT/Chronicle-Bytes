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
package net.openhft.chronicle.bytes.pool;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.core.scoped.ScopedResource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class BytesPoolTest {

    @Test
    void testAcquireBytes() {
        try (ScopedResource<Bytes<?>> resource = BytesPool.createThreadLocal().get()) {
            Bytes<?> bytes = resource.get();
            assertNotNull(bytes, "Acquired bytes should not be null.");

            assertEquals(0, bytes.readRemaining(), "Acquired bytes should be ready for use.");
        }
    }

    @Test
    void testBytesPoolUsage() {
        try (ScopedResource<Bytes<?>> resource = BytesPool.createThreadLocal().get()) {
            Bytes<?> bytes = resource.get();

            bytes.writeUtf8("Hello, World!");
            assertEquals("Hello, World!", bytes.readUtf8());

            bytes.clear();

            assertEquals(0, bytes.readRemaining());

            bytes.releaseLast();
        }
    }
}
