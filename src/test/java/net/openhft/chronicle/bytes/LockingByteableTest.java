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
import static org.junit.jupiter.api.Assumptions.*;

class LockingByteableTest extends BytesTestCommon {
    @Test
    void notLockable() throws IOException {
        try (BinaryLongReference blr = new BinaryLongReference()) {
            blr.bytesStore(Bytes.from("Hello World"), 0, 8);
            assertThrows(UnsupportedOperationException.class, () -> blr.lock(false));
        }
    }

    @Test
    void lockableShared() throws IOException {
        assumeFalse(Jvm.maxDirectMemory() == 0);

        final String tmp = IOTools.tempName("lockableShared");
        new File(tmp).deleteOnExit();

        for (int i = 0; i < 3; i++) {
            try (MappedBytes mbs = MappedBytes.mappedBytes(tmp, 64 << 10);
                 BinaryLongReference blr = new BinaryLongReference()) {
                blr.bytesStore(mbs, 0, 8);
                try (FileLock fl = blr.lock(true)) {
                    assertNotNull(fl);
                }
                try (FileLock fl = blr.lock(true)) {
                    assertNotNull(fl);
                }
            }
        }
    }

    @Test
    void tryLockableShared() throws IOException {
        assumeFalse(Jvm.maxDirectMemory() == 0);

        final String tmp = IOTools.tempName("lockableShared");
        new File(tmp).deleteOnExit();

        for (int i = 0; i < 3; i++) {
            try (MappedBytes mbs = MappedBytes.mappedBytes(tmp, 64 << 10);
                 BinaryLongReference blr = new BinaryLongReference()) {
                blr.bytesStore(mbs, 0, 8);
                try (FileLock fl = blr.tryLock(true)) {
                    assertNotNull(fl);
                }
                try (FileLock fl = blr.tryLock(true)) {
                    assertNotNull(fl);
                }
            }
        }
    }

    @Test
    void doubleLockableShared() throws IOException {
        assumeFalse(Jvm.maxDirectMemory() == 0);

        assertThrows(OverlappingFileLockException.class, () -> {
            final String tmp = IOTools.tempName("doubleLockableShared");
            new File(tmp).deleteOnExit();

            try (MappedBytes mbs = MappedBytes.mappedBytes(tmp, 64 << 10);
                 BinaryLongReference blr = new BinaryLongReference()) {
                blr.bytesStore(mbs, 0, 8);
                try (FileLock fl = blr.lock(true)) {
                    blr.lock(false);
                    fail();
                    assertNotNull(fl); // keep compiler happy.
                }
            }
        });
    }

    @Test
    void lockableSharedSingle() throws IOException {
        assumeFalse(Jvm.maxDirectMemory() == 0);

        final String tmp = IOTools.tempName("lockableShared");
        new File(tmp).deleteOnExit();

        for (int i = 0; i < 3; i++) {
            try (MappedBytes mbs = MappedBytes.singleMappedBytes(tmp, 64 << 10);
                 BinaryLongReference blr = new BinaryLongReference()) {
                blr.bytesStore(mbs, 0, 8);
                try (FileLock fl = blr.lock(true)) {
                    assertNotNull(fl);
                }
                try (FileLock fl = blr.lock(true)) {
                    assertNotNull(fl);
                }
            }
        }
    }

    @Test
    void tryLockableSharedSingle() throws IOException {
        assumeFalse(Jvm.maxDirectMemory() == 0);

        final String tmp = IOTools.tempName("lockableShared");
        new File(tmp).deleteOnExit();

        for (int i = 0; i < 3; i++) {
            try (MappedBytes mbs = MappedBytes.singleMappedBytes(tmp, 64 << 10);
                 BinaryLongReference blr = new BinaryLongReference()) {
                blr.bytesStore(mbs, 0, 8);
                try (FileLock fl = blr.tryLock(true)) {
                    assertNotNull(fl);
                }
                try (FileLock fl = blr.tryLock(true)) {
                    assertNotNull(fl);
                }
            }
        }
    }

    @Test
    void doubleLockableSharedSingle() throws IOException {
        assumeFalse(Jvm.maxDirectMemory() == 0);

        assertThrows(OverlappingFileLockException.class, () -> {
            final String tmp = IOTools.tempName("doubleLockableShared");
            new File(tmp).deleteOnExit();

            try (MappedBytes mbs = MappedBytes.singleMappedBytes(tmp, 64 << 10);
                 BinaryLongReference blr = new BinaryLongReference()) {
                blr.bytesStore(mbs, 0, 8);
                try (FileLock fl = blr.lock(true)) {
                    blr.lock(false);
                    fail();
                    assertNotNull(fl); // keep compiler happy.
                }
            }
        });
    }
}
