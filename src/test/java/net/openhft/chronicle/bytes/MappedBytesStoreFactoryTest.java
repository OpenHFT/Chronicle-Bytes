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

import net.openhft.chronicle.core.io.ClosedIllegalStateException;
import net.openhft.chronicle.core.io.ReferenceOwner;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.File;

import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.*;

public class MappedBytesStoreFactoryTest {

    @Mock
    private MappedBytesStoreFactory factory;

    @Mock
    private ReferenceOwner owner;

    @Mock
    private MappedFile mappedFile;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        when(mappedFile.file()).thenReturn(new File("test"));
        when(factory.create(eq(owner), eq(mappedFile), anyLong(), anyLong(), anyLong(), anyLong(), anyInt()))
                .thenReturn(mock(MappedBytesStore.class));
    }

    @Test
    public void createMappedBytesStoreWithValidParameters() throws ClosedIllegalStateException {
        long start = 0L;
        long address = 1024L;
        long capacity = 4096L;
        long safeCapacity = 2048L;
        int pageSize = 4096;

        MappedBytesStore store = factory.create(owner, mappedFile, start, address, capacity, safeCapacity, pageSize);
        assertNotNull(store);
        verify(factory, times(1)).create(owner, mappedFile, start, address, capacity, safeCapacity, pageSize);
    }

    @Test(expected = ClosedIllegalStateException.class)
    public void createMappedBytesStoreWhenFileClosed() throws ClosedIllegalStateException {
        when(factory.create(any(), any(), anyLong(), anyLong(), anyLong(), anyLong(), anyInt()))
                .thenThrow(new ClosedIllegalStateException("MappedFile has been released"));

        factory.create(owner, mappedFile, 0, 0, 0, 0, PageUtil.getPageSize("test"));
    }
}
