/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes;

import net.openhft.chronicle.bytes.ref.BinaryLongReference;
import net.openhft.chronicle.core.Jvm;
import net.openhft.chronicle.core.io.IOTools;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

@SuppressWarnings("checkstyle:MMOverusedWord")
@DisplayName("File locking behaviour for binary references")
public class LockingByteableTest extends BytesTestCommon {
    @Test
    @DisplayName("non lockable store rejects lock attempt")
    public void notLockable() throws IOException {
        assertThrows(UnsupportedOperationException.class, () -> {
            try (BinaryLongReference blr = new BinaryLongReference()) {
                blr.bytesStore(Bytes.from("Hello World"), 0, 8);
                blr.lock(false);
            }
        }, "Non lockable store rejects lock operation");
    }

    @Test
    @DisplayName("shared locking returns lock handle repeatedly")
    public void lockableShared() throws IOException {
        assumeFalse(Jvm.maxDirectMemory() == 0,
                "Direct memory must be available for shared lock test");

        final String tmp = IOTools.tempName("lockableShared");
        new File(tmp).deleteOnExit();

        for (int i = 0; i < 3; i++) {
            try (MappedBytes mbs = MappedBytes.mappedBytes(tmp, 64 << 10);
                 BinaryLongReference blr = new BinaryLongReference()) {
                blr.bytesStore(mbs, 0, 8);
                try (FileLock fl = blr.lock(true)) {
                    assertNotNull(fl,
                            "Shared lock attempt returns a handle on iteration " + i);
                }
                try (FileLock fl = blr.lock(true)) {
                    assertNotNull(fl,
                            "Shared lock reentry returns a handle on iteration " + i);
                }
            }
        }
    }

    @Test
    @DisplayName("shared tryLock returns lock handle repeatedly")
    public void tryLockableShared() throws IOException {
        assumeFalse(Jvm.maxDirectMemory() == 0,
                "Direct memory must be available for shared tryLock test");

        final String tmp = IOTools.tempName("lockableShared");
        new File(tmp).deleteOnExit();

        for (int i = 0; i < 3; i++) {
            try (MappedBytes mbs = MappedBytes.mappedBytes(tmp, 64 << 10);
                 BinaryLongReference blr = new BinaryLongReference()) {
                blr.bytesStore(mbs, 0, 8);
                try (FileLock fl = blr.tryLock(true)) {
                    assertNotNull(fl,
                            "Shared tryLock returns a handle on iteration " + i);
                }
                try (FileLock fl = blr.tryLock(true)) {
                    assertNotNull(fl,
                            "Shared tryLock reentry returns a handle on iteration " + i);
                }
            }
        }
    }

    @Test
    @DisplayName("overlapping shared lock raises file lock exception")
    public void doubleLockableShared() throws IOException {
        assumeFalse(Jvm.maxDirectMemory() == 0,
                "Direct memory must be available for overlapping lock test");

        final String tmp = IOTools.tempName("doubleLockableShared");
        new File(tmp).deleteOnExit();

        try (MappedBytes mbs = MappedBytes.mappedBytes(tmp, 64 << 10);
             BinaryLongReference blr = new BinaryLongReference()) {
            blr.bytesStore(mbs, 0, 8);
            assertThrows(OverlappingFileLockException.class, () -> {
                try (FileLock fl = blr.lock(true)) {
                    assertNotNull(fl, "Initial shared lock should be acquired");
                    blr.lock(false);
                }
            }, "Overlapping lock attempt should raise file lock exception");
        }
    }

    @Test
    @DisplayName("shared single mapped locking returns lock handle")
    public void lockableSharedSingle() throws IOException {
        assumeFalse(Jvm.maxDirectMemory() == 0,
                "Direct memory must be available for shared single lock test");

        final String tmp = IOTools.tempName("lockableShared");
        new File(tmp).deleteOnExit();

        for (int i = 0; i < 3; i++) {
            try (MappedBytes mbs = MappedBytes.singleMappedBytes(tmp, 64 << 10);
                 BinaryLongReference blr = new BinaryLongReference()) {
                blr.bytesStore(mbs, 0, 8);
                try (FileLock fl = blr.lock(true)) {
                    assertNotNull(fl,
                            "Single mapped lock returns a handle on iteration " + i);
                }
                try (FileLock fl = blr.lock(true)) {
                    assertNotNull(fl,
                            "Single mapped lock reentry returns a handle on iteration " + i);
                }
            }
        }
    }

    @Test
    @DisplayName("shared single mapped tryLock returns lock handle")
    public void tryLockableSharedSingle() throws IOException {
        assumeFalse(Jvm.maxDirectMemory() == 0,
                "Direct memory must be available for shared single tryLock test");

        final String tmp = IOTools.tempName("lockableShared");
        new File(tmp).deleteOnExit();

        for (int i = 0; i < 3; i++) {
            try (MappedBytes mbs = MappedBytes.singleMappedBytes(tmp, 64 << 10);
                 BinaryLongReference blr = new BinaryLongReference()) {
                blr.bytesStore(mbs, 0, 8);
                try (FileLock fl = blr.tryLock(true)) {
                    assertNotNull(fl,
                            "Single mapped tryLock returns a handle on iteration " + i);
                }
                try (FileLock fl = blr.tryLock(true)) {
                    assertNotNull(fl,
                            "Single mapped tryLock reentry returns a handle on iteration " + i);
                }
            }
        }
    }

    @Test
    @DisplayName("overlapping single mapped lock raises exception")
    public void doubleLockableSharedSingle() throws IOException {
        assumeFalse(Jvm.maxDirectMemory() == 0,
                "Direct memory must be available for overlapping single lock test");

        final String tmp = IOTools.tempName("doubleLockableShared");
        new File(tmp).deleteOnExit();

        try (MappedBytes mbs = MappedBytes.singleMappedBytes(tmp, 64 << 10);
             BinaryLongReference blr = new BinaryLongReference()) {
            blr.bytesStore(mbs, 0, 8);
            assertThrows(OverlappingFileLockException.class, () -> {
                try (FileLock fl = blr.lock(true)) {
                    assertNotNull(fl, "Initial single mapped lock should be acquired");
                    blr.lock(false);
                }
            }, "Overlapping single lock attempt should raise file lock exception");
        }
    }
}
