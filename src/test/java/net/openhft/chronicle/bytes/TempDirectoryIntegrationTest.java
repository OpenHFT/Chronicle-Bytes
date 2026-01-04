/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes;

import net.openhft.chronicle.core.OS;
import net.openhft.chronicle.core.io.IOTools;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TempDirectoryIntegrationTest extends BytesTestCommon {

    @Test
    public void createTempDirectoryUnderTargetAndCleanup() throws Exception {
        final Path tempDir = IOTools.createTempDirectory("bytes-temp");
        final Path targetRoot = new File(OS.getTarget()).getAbsoluteFile().toPath().normalize();
        assertTrue(tempDir.toAbsolutePath().normalize().startsWith(targetRoot), "Temp directory should live under OS target");

        Files.createDirectories(tempDir);
        assertTrue(Files.isDirectory(tempDir), "Temp directory should exist");
        final Path marker = tempDir.resolve("marker.bin");
        Files.write(marker, new byte[]{1, 2, 3});
        assertTrue(Files.exists(marker), "Marker file should exist inside temp dir");

        assertTrue(IOTools.deleteDirWithFiles(tempDir.toFile()), "Temp directory deletion should succeed");
        assertFalse(Files.exists(tempDir), "Temp directory should be removed");
    }
}
