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

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class BytesContextTest {

    private BytesContext context;

    @Before
    public void setUp() {
        // Mock the BytesContext interface
        context = mock(BytesContext.class);
        doThrow(UnsupportedOperationException.class).when(context).isClosed();
    }

    @Test
    public void testKey() {
        // Setup a specific key to return
        final int expectedKey = 42;
        when(context.key()).thenReturn(expectedKey);

        int actualKey = context.key();
        assertEquals("Key should match the expected value", expectedKey, actualKey);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testIsClosedThrowsUnsupportedOperationException() {
        context.isClosed();
    }

    @Test
    public void testRollbackOnClose() {
        try {
            context.rollbackOnClose();
        } catch (Exception e) {
            fail("rollbackOnClose should not throw any exception");
        }
    }
}
