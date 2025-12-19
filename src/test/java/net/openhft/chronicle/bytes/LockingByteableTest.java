/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes;

import net.openhft.chronicle.bytes.ref.BinaryLongReference;
import net.openhft.chronicle.core.Jvm;
import net.openhft.chronicle.core.io.IOTools;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

public class LockingByteableTest extends BytesTestCommon {
    @Test
    public void notLockable() throws IOException {
        assertThrows(UnsupportedOperationException.class, () -> {
            try (BinaryLongReference blr = new BinaryLongReference()) {
                blr.bytesStore(Bytes.from("Hello World"), 0, 8);
                blr.lock(false);
            }
        });
    }

    @Test
    public void lockableShared() throws IOException {
        assumeFalse(Jvm.maxDirectMemory() == 0);

        final String tmp = IOTools.tempName("lockableShared");
        new File(tmp).deleteOnExit();

        for (int i = 0; i < 3; i++) {
            try (MappedBytes mbs = MappedBytes.mappedBytes(tmp, 64 << 10);
                 BinaryLongReference blr = new BinaryLongReference()) {
                blr.bytesStore(mbs, 0, 8);
                try (FileLock fl = blr.lock(true)) {
                    assertNotNull(fl, "shared lock should be acquired successfully");
                }
                try (FileLock fl = blr.lock(true)) {
                    assertNotNull(fl, "shared lock should be re-acquired after release");
                }
            }
        }
    }

    @Test
    public void tryLockableShared() throws IOException {
        assumeFalse(Jvm.maxDirectMemory() == 0);

        final String tmp = IOTools.tempName("lockableShared");
        new File(tmp).deleteOnExit();

        for (int i = 0; i < 3; i++) {
            try (MappedBytes mbs = MappedBytes.mappedBytes(tmp, 64 << 10);
                 BinaryLongReference blr = new BinaryLongReference()) {
                blr.bytesStore(mbs, 0, 8);
                try (FileLock fl = blr.tryLock(true)) {
                    assertNotNull(fl, "tryLock with shared mode should succeed");
                }
                try (FileLock fl = blr.tryLock(true)) {
                    assertNotNull(fl, "tryLock with shared mode should succeed after release");
                }
            }
        }
    }

    @Test
    public void doubleLockableShared() throws IOException {
        assertThrows(OverlappingFileLockException.class, () -> {
            assumeFalse(Jvm.maxDirectMemory() == 0);

            final String tmp = IOTools.tempName("doubleLockableShared");
            new File(tmp).deleteOnExit();

            try (MappedBytes mbs = MappedBytes.mappedBytes(tmp, 64 << 10);
                 BinaryLongReference blr = new BinaryLongReference()) {
                blr.bytesStore(mbs, 0, 8);
                try (FileLock fl = blr.lock(true)) {
                    blr.lock(false);
                    fail("acquiring exclusive lock while holding shared lock should throw OverlappingFileLockException");
                    assertNotNull(fl, "first lock should be acquired"); // keep compiler happy.
                }
            }
        });
    }

    @Test
    public void lockableSharedSingle() throws IOException {
        assumeFalse(Jvm.maxDirectMemory() == 0);

        final String tmp = IOTools.tempName("lockableShared");
        new File(tmp).deleteOnExit();

        for (int i = 0; i < 3; i++) {
            try (MappedBytes mbs = MappedBytes.singleMappedBytes(tmp, 64 << 10);
                 BinaryLongReference blr = new BinaryLongReference()) {
                blr.bytesStore(mbs, 0, 8);
                try (FileLock fl = blr.lock(true)) {
                    assertNotNull(fl, "shared lock should be acquired on singleMappedBytes");
                }
                try (FileLock fl = blr.lock(true)) {
                    assertNotNull(fl, "shared lock should be re-acquired on singleMappedBytes after release");
                }
            }
        }
    }

    @Test
    public void tryLockableSharedSingle() throws IOException {
        assumeFalse(Jvm.maxDirectMemory() == 0);

        final String tmp = IOTools.tempName("lockableShared");
        new File(tmp).deleteOnExit();

        for (int i = 0; i < 3; i++) {
            try (MappedBytes mbs = MappedBytes.singleMappedBytes(tmp, 64 << 10);
                 BinaryLongReference blr = new BinaryLongReference()) {
                blr.bytesStore(mbs, 0, 8);
                try (FileLock fl = blr.tryLock(true)) {
                    assertNotNull(fl, "tryLock should succeed on singleMappedBytes");
                }
                try (FileLock fl = blr.tryLock(true)) {
                    assertNotNull(fl, "tryLock should succeed on singleMappedBytes after release");
                }
            }
        }
    }

    @Test
    public void doubleLockableSharedSingle() throws IOException {
        assertThrows(OverlappingFileLockException.class, () -> {
            assumeFalse(Jvm.maxDirectMemory() == 0);

            final String tmp = IOTools.tempName("doubleLockableShared");
            new File(tmp).deleteOnExit();

            try (MappedBytes mbs = MappedBytes.singleMappedBytes(tmp, 64 << 10);
                 BinaryLongReference blr = new BinaryLongReference()) {
                blr.bytesStore(mbs, 0, 8);
                try (FileLock fl = blr.lock(true)) {
                    blr.lock(false);
                    fail("acquiring exclusive lock on singleMappedBytes while holding shared lock should throw OverlappingFileLockException");
                    assertNotNull(fl, "first lock should be acquired"); // keep compiler happy.
                }
            }
        });
    }
}
