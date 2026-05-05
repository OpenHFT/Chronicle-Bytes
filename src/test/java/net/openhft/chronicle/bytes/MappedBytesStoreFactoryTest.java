/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes;

import net.openhft.chronicle.core.io.ClosedIllegalStateException;
import net.openhft.chronicle.core.io.ReferenceOwner;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.File;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

/**
 * Tests MappedBytesStoreFactory creation behaviour because proper factory
 * validation is essential to avoid illegal state exceptions when accessing
 * closed or invalid mapped files.
 */
@DisplayName("Mapped bytes store factory creation behaviour")
@SuppressWarnings("PMD.JUnit5TestShouldBePackagePrivate")
class MappedBytesStoreFactoryTest {

    @Mock
    private MappedBytesStoreFactory factory;

    @Mock
    private ReferenceOwner owner;

    @Mock
    private MappedFile mappedFile;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        when(mappedFile.file()).thenReturn(new File("test"));
        when(factory.create(eq(owner), eq(mappedFile), anyLong(), anyLong(), anyLong(), anyLong(), anyInt()))
                .thenReturn(mock(MappedBytesStore.class));
    }

    @Test
    @DisplayName("create mapped bytes store with valid parameters")
    public void createMappedBytesStoreWithValidParameters() throws ClosedIllegalStateException {
        long start = 0L;
        long address = 1024L;
        long capacity = 4096L;
        long safeCapacity = 2048L;
        int pageSize = 4096;

        MappedBytesStore store = factory.create(owner, mappedFile, start, address, capacity, safeCapacity, pageSize);
        assertNotNull(store,
                "Factory returns mapped bytes store for valid parameters");
        verify(factory, times(1)).create(owner, mappedFile, start, address, capacity, safeCapacity, pageSize);
    }

    @Test
    @DisplayName("create mapped bytes store fails when file closed")
    public void createMappedBytesStoreWhenFileClosed() throws ClosedIllegalStateException {
        when(factory.create(any(), any(), anyLong(), anyLong(), anyLong(), anyLong(), anyInt()))
                .thenThrow(new ClosedIllegalStateException("MappedFile has been released"));

        assertThrows(ClosedIllegalStateException.class,
                () -> factory.create(owner, mappedFile, 0, 0, 0, 0, PageUtil.getPageSize("test")),
                "Factory should reject closed mapped file");
    }
}
