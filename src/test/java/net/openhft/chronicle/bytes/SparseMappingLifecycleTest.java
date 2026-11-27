/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes;

import static org.junit.Assert.*;

import net.openhft.chronicle.core.OS;
import net.openhft.chronicle.core.io.ReferenceOwner;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.*;

public class SparseMappingLifecycleTest extends BytesTestCommon {

    @Rule public TemporaryFolder temporary = new TemporaryFolder();

    private long chunk() {
        return OS.mapAlign(4096);
    }

    @Test
    public void syncModeAllowsSparseAcquisition() throws Exception {
        ReferenceOwner owner = ReferenceOwner.temporary("sparse");
        try (MappedFile file = MappedFile.of(temporary.newFile(), chunk(), 0, false)) {
            MappedBytesStore store = file.acquireByteStore(owner, 2 * chunk());
            try {
                file.syncMode(SyncMode.NONE);
                assertEquals(SyncMode.NONE, store.syncMode());
            } finally {
                store.release(owner);
            }
        }
    }

    @Test
    public void mappedStoreOutlivesFileOwner() throws Exception {
        ReferenceOwner owner = ReferenceOwner.temporary("external");
        MappedFile file = MappedFile.of(temporary.newFile(), chunk(), 0, false);
        MappedBytesStore store = file.acquireByteStore(owner, 2 * chunk());
        try {
            store.writeLong(2 * chunk(), 123);
            file.close();
            assertEquals(123, store.readLong(2 * chunk()));
        } finally {
            store.release(owner);
            file.close();
        }
    }

    @Test
    public void singleMappingGrowthPreservesExistingPrefix() throws Exception {
        File path = temporary.newFile();
        try (RandomAccessFile raf = new RandomAccessFile(path, "rw")) {
            raf.writeLong(0x0102030405060708L);
        }
        try (MappedFile file = MappedFile.ofSingle(path, chunk(), false)) {
            assertTrue(file.capacity() >= chunk());
            assertTrue(path.length() >= chunk());
        }
        try (RandomAccessFile raf = new RandomAccessFile(path, "r")) {
            assertEquals(0x0102030405060708L, raf.readLong());
        }
    }

    @Test
    public void chunkGrowthPreservesEarlierData() throws Exception {
        ReferenceOwner owner = ReferenceOwner.temporary("growth");
        try (MappedFile file = MappedFile.of(temporary.newFile(), chunk(), 0, false)) {
            MappedBytesStore first = file.acquireByteStore(owner, 0);
            try {
                first.writeLong(0, 123);
                MappedBytesStore later = file.acquireByteStore(owner, 3 * chunk());
                try {
                    later.writeLong(3 * chunk(), 456);
                    assertEquals(123, first.readLong(0));
                } finally {
                    later.release(owner);
                }
            } finally {
                first.release(owner);
            }
        }
    }
}
