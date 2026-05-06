/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes;

import net.openhft.chronicle.core.Jvm;
import net.openhft.chronicle.core.OS;
import net.openhft.chronicle.core.io.BackgroundResourceReleaser;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystemException;
import java.nio.file.Files;

import static org.junit.Assert.assertEquals;
import static org.junit.Assume.assumeFalse;

public class MappedBytesWrite8bitBoundaryTest extends BytesTestCommon {

    @Test
    public void write8bitAcrossChunkBoundary() throws IOException {
        assumeFalse(Jvm.maxDirectMemory() == 0);
        // Use page size as chunk to make boundary deterministic
        final int chunk = OS.pageSize();
        File file = new File(OS.getTarget(), "mapped-write8bit-boundary-" + System.nanoTime() + ".dat");
        Files.createDirectories(file.getParentFile().toPath());
        String msg = repeat('A', 32);
        try {
            try (MappedBytes mb = MappedBytes.mappedBytes(file, chunk)) {
                // position at end of first chunk minus a few bytes so the encoded length + data cross
                mb.writePosition(chunk - 2);
                mb.write8bit(msg);
                mb.readPosition(chunk - 2);
                String got = mb.read8bit();
                assertEquals(msg, got);
            }
        } finally {
            BackgroundResourceReleaser.releasePendingResources();
            Files.deleteIfExists(file.toPath());
        }
    }

    private static String repeat(char c, int n) {
        byte[] b = new byte[n];
        for (int i = 0; i < n; i++) b[i] = (byte) c;
        return new String(b, StandardCharsets.ISO_8859_1);
    }
}
