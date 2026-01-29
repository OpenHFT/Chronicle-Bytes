/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes;

import net.openhft.chronicle.core.Jvm;
import net.openhft.chronicle.core.OS;
import net.openhft.chronicle.core.io.IORuntimeException;
import net.openhft.chronicle.core.io.IOTools;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

/**
 * Tests mapped bytes view selection and sync operations because these
 * code paths require coverage to avoid regressions in memory-mapped file
 * handling.
 */
@DisplayName("MappedBytes covers view selection and sync branch scenarios")
public class MappedBytesCoverageTest extends BytesTestCommon {

    @Test
    @DisplayName("bytesForRead switches view based on clear state")
    public void bytesForReadUsesExpectedView() throws Exception {
        assumeFalse(Jvm.maxDirectMemory() == 0,
                "Direct memory must be available for mapped bytes views");

        File file = IOTools.createTempFile("mappedBytesView");
        try (MappedBytes mappedBytes = MappedBytes.mappedBytes(file, OS.pageSize())) {
            Bytes<Void> view = mappedBytes.bytesForRead();
            try {
                assertTrue(view instanceof VanillaBytes,
                        "Clear mapped bytes should return a VanillaBytes view");
            } finally {
                view.releaseLast();
            }

            mappedBytes.writePosition(1);
            mappedBytes.readPosition(1);
            Bytes<Void> advancedView = mappedBytes.bytesForRead();
            try {
                assertTrue(advancedView instanceof SubBytes,
                        "Advanced read position should return a SubBytes view");
            } finally {
                advancedView.releaseLast();
            }

            mappedBytes.sync();
        } finally {
            Path path = file.toPath();
            try {
                Files.deleteIfExists(path);
            } catch (IOException e) {
                throw new IORuntimeException("Unable to delete temp file: " + path, e);
            }
        }
    }

    @Test
    @DisplayName("sync ignores non mapped stores without backing file")
    public void syncIgnoresNonMappedStore() throws Exception {
        MappedBytes bytes = new MappedBytes("test") {
            @Override
            public BytesStore<Bytes<Void>, Void> copy() {
                return BytesStore.empty();
            }

            @Override
            public void close() {
                // no-op for coverage-only stub
            }

            @Override
            public boolean isClosed() {
                return false;
            }

            @Override
            public boolean isBackingFileReadOnly() {
                return false;
            }

            @Override
            public void chunkCount(long[] chunkCount) {
                chunkCount[0] = 0L;
            }

            @Override
            public MappedFile mappedFile() {
                // Test stub: return null to simulate detached state
                return null;
            }
        };
        try {
            bytes.sync();
        } finally {
            bytes.releaseLast();
        }
    }
}
