/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes;

import net.openhft.chronicle.core.OS;
import net.openhft.chronicle.core.io.BackgroundResourceReleaser;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;

import static org.junit.Assert.*;

public class TimestampFileSetupTest extends BytesTestCommon {
    @Rule public final TemporaryFolder directory = new TemporaryFolder();

    @Test
    public void createsMissingTimestampFile() throws Exception {
        File file = new File(directory.getRoot(), "timestamp");
        assertFalse(file.exists());
        DistributedUniqueTimeProviderTest.ensureTimestampFile(file);
        assertTrue(file.isFile());
        assertEquals(0, file.length());
    }

    @Test
    public void preservesMappedTimestampAndItsOwner() throws Exception {
        File file = directory.newFile("mapped-timestamp");
        long marker = 0x123456789abcdefL;
        try (MappedBytes bytes = MappedBytes.mappedBytes(file, OS.SAFE_PAGE_SIZE)) {
            bytes.writeLong(0, marker);
            long length = file.length();
            DistributedUniqueTimeProviderTest.ensureTimestampFile(file);
            assertEquals(length, file.length());
            assertEquals(marker, bytes.readLong(0));
            bytes.writeLong(0, marker + 1);
            assertEquals(marker + 1, bytes.readLong(0));
        } finally {
            BackgroundResourceReleaser.releasePendingResources();
        }
        assertTrue("Mapped owner was not released", file.delete());
    }
}
