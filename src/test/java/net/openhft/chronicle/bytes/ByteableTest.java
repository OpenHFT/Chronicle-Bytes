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
package net.openhft.chronicle.bytes;

import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class ByteableTest {

    private Byteable byteable;
    private BytesStore<?, ?> bytesStore;

    @SuppressWarnings("unchecked")
    @Before
    public void setUp() throws IOException {
        byteable = mock(Byteable.class);
        bytesStore = mock(BytesStore.class);
        doThrow(UnsupportedOperationException.class).when(byteable).address();
        doThrow(UnsupportedOperationException.class).when(byteable).lock(true);
        doThrow(UnsupportedOperationException.class).when(byteable).tryLock(true);
    }

    @Test
    public void testOffset() {
        long expectedOffset = 5L;
        when(byteable.offset()).thenReturn(expectedOffset);

        assertEquals(expectedOffset, byteable.offset());
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testAddressThrowsUnsupportedOperationException() {
        when(byteable.address()).thenCallRealMethod();
        byteable.address();
    }

    @Test
    public void testMaxSize() {
        long expectedSize = 1024L;
        when(byteable.maxSize()).thenReturn(expectedSize);

        assertEquals(expectedSize, byteable.maxSize());
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testLockThrowsUnsupportedOperationException() throws IOException {
        byteable.lock(true);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testTryLockThrowsUnsupportedOperationException() throws IOException {
        byteable.tryLock(true);
    }
}
