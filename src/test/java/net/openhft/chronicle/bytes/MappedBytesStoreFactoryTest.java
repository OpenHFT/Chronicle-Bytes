/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes;

import net.openhft.chronicle.core.io.ClosedIllegalStateException;
import net.openhft.chronicle.core.io.ReferenceOwner;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.io.File;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class MappedBytesStoreFactoryTest {

    @Mock
    private MappedBytesStoreFactory factory;

    @Mock
    private ReferenceOwner owner;

    @Mock
    private MappedFile mappedFile;

    @BeforeEach
    public void setUp() {
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

    @Test
    public void createMappedBytesStoreWhenFileClosed() {
        assertThrows(ClosedIllegalStateException.class, () -> {
            when(factory.create(any(), any(), anyLong(), anyLong(), anyLong(), anyLong(), anyInt()))
                    .thenThrow(new ClosedIllegalStateException("MappedFile has been released"));

            factory.create(owner, mappedFile, 0, 0, 0, 0, PageUtil.getPageSize("test"));
        });
    }
}
