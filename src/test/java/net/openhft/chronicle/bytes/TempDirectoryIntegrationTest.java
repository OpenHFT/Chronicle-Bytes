/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes;

import net.openhft.chronicle.core.OS;
import net.openhft.chronicle.core.io.IOTools;
import org.junit.Test;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class TempDirectoryIntegrationTest extends BytesTestCommon {

    @Test
    public void createTempDirectoryUnderTargetAndCleanup() throws Exception {
        final Path tempDir = IOTools.createTempDirectory("bytes-temp");
        final Path targetRoot = new File(OS.getTarget()).getAbsoluteFile().toPath().normalize();
        assertTrue("Temp directory should live under OS target",
                tempDir.toAbsolutePath().normalize().startsWith(targetRoot));

        Files.createDirectories(tempDir);
        assertTrue("Temp directory should exist", Files.isDirectory(tempDir));
        final Path marker = tempDir.resolve("marker.bin");
        Files.write(marker, new byte[]{1, 2, 3});
        assertTrue("Marker file should exist inside temp dir", Files.exists(marker));

        assertTrue("Temp directory deletion should succeed", IOTools.deleteDirWithFiles(tempDir.toFile()));
        assertFalse("Temp directory should be removed", Files.exists(tempDir));
    }
}
