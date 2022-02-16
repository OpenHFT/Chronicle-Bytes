package net.openhft.chronicle.bytes;

import net.openhft.chronicle.bytes.ref.BinaryLongReference;
import net.openhft.chronicle.core.io.IOTools;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

public class LockingByteableTest extends BytesTestCommon {
    @Test(expected = UnsupportedOperationException.class)
    public void notLockable() throws IOException {
        try (BinaryLongReference blr = new BinaryLongReference()) {
            blr.bytesStore(Bytes.from("Hello World"), 0, 8);
            blr.lock(false);
        }
    }

    @Test
    public void lockableShared() throws IOException {
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
    public void tryLockableShared() throws IOException {
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

    @Test(expected = OverlappingFileLockException.class)
    public void doubleLockableShared() throws IOException {
        final String tmp = IOTools.tempName("doubleLockableShared");
        new File(tmp).deleteOnExit();

        try (MappedBytes mbs = MappedBytes.mappedBytes(tmp, 64 << 10);
             BinaryLongReference blr = new BinaryLongReference()) {
            blr.bytesStore(mbs, 0, 8);
            try (FileLock fl = blr.lock(true)) {
                blr.lock(false);
                fail();
            }
        }
    }


    @Test
    public void lockableSharedSingle() throws IOException {
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
    public void tryLockableSharedSingle() throws IOException {
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

    @Test(expected = OverlappingFileLockException.class)
    public void doubleLockableSharedSingle() throws IOException {
        final String tmp = IOTools.tempName("doubleLockableShared");
        new File(tmp).deleteOnExit();

        try (MappedBytes mbs = MappedBytes.singleMappedBytes(tmp, 64 << 10);
             BinaryLongReference blr = new BinaryLongReference()) {
            blr.bytesStore(mbs, 0, 8);
            try (FileLock fl = blr.lock(true)) {
                blr.lock(false);
                fail();
            }
        }
    }
}
