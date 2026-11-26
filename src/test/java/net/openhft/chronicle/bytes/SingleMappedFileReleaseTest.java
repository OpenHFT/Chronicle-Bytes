/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes;

import net.openhft.chronicle.core.io.BackgroundResourceReleaser;
import net.openhft.chronicle.core.io.ReferenceOwner;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Collection;

import static org.junit.Assert.assertEquals;

@RunWith(Parameterized.class)
public class SingleMappedFileReleaseTest extends BytesTestCommon {
    private final boolean readOnly;

    public SingleMappedFileReleaseTest(boolean readOnly) {
        this.readOnly = readOnly;
    }

    @Parameterized.Parameters(name = "readOnly={0}")
    public static Collection<Object[]> modes() {
        return Arrays.asList(new Object[]{false}, new Object[]{true});
    }

    @Test
    public void closingBytesReleasesTheFileAndStore() throws IOException {
        File file = fixture();
        MappedBytes bytes = MappedBytes.singleMappedBytes(file, 64 << 10, readOnly);
        MappedFile mappedFile = bytes.mappedFile();
        BytesStore<?, ?> store = bytes.bytesStore();
        bytes.close();
        BackgroundResourceReleaser.releasePendingResources();

        assertEquals(0, bytes.refCount());
        assertEquals(0, mappedFile.refCount());
        assertEquals("The file must release its store reservation", 0, store.refCount());
        Files.delete(file.toPath());
    }

    @Test
    public void borrowedStoreSurvivesFileCloseUntilItsOwnerReleasesIt() throws IOException {
        File file = fixture();
        ReferenceOwner owner = ReferenceOwner.temporary("borrowed-store");
        MappedFile mappedFile = MappedFile.ofSingle(file, 64 << 10, readOnly);
        MappedBytesStore store = mappedFile.acquireByteStore(owner, 0);
        try {
            mappedFile.close();
            BackgroundResourceReleaser.releasePendingResources();
            assertEquals("Only the borrower should remain", 1, store.refCount());
            assertEquals(42, store.readByte(0));
        } finally {
            store.release(owner);
            BackgroundResourceReleaser.releasePendingResources();
        }
        assertEquals(0, store.refCount());
        Files.delete(file.toPath());
    }

    private static File fixture() throws IOException {
        File file = Files.createTempFile("single-mapping-release-", ".bin").toFile();
        file.deleteOnExit();
        try (RandomAccessFile raf = new RandomAccessFile(file, "rw")) {
            raf.setLength(64 << 10);
            raf.writeByte(42);
        }
        return file;
    }
}
