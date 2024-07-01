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
import static org.mockito.Mockito.*;

public class RingBufferReaderTest {

    private RingBufferReader reader;

    @Before
    public void setUp() {
        reader = mock(RingBufferReader.class);
    }

    @Test
    public void testIsEmpty() {
        when(reader.isEmpty()).thenReturn(true);

        assert(reader.isEmpty());

        verify(reader, times(1)).isEmpty();
    }

    @Test
    public void testIsStopped() {
        when(reader.isStopped()).thenReturn(false);

        assert(!reader.isStopped());

        verify(reader, times(1)).isStopped();
    }

    @Test
    public void testStop() {
        reader.stop();

        verify(reader, times(1)).stop();
    }
}
