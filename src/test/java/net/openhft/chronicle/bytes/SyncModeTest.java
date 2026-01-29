/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes;

import net.openhft.chronicle.core.Jvm;
import net.openhft.chronicle.core.io.IOTools;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.stream.Stream;

import static net.openhft.chronicle.core.Jvm.uncheckedCast;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

/**
 * Tests sync mode application because correct durability settings are
 * essential to ensure data reaches persistent storage as expected.
 */
@DisplayName("SyncMode - validates mapped file sync mode propagation")
public class SyncModeTest extends BytesTestCommon {
    public static Stream<SyncMode> parameters() {
        return Stream.of(SyncMode.values());
    }

    @ParameterizedTest(name = "{0}")
    @DisplayName("mapped file sync mode applies to bytes store")
    @MethodSource("parameters")
    public void largeFile(SyncMode syncMode) throws FileNotFoundException {
        assumeFalse(Jvm.maxDirectMemory() == 0, "Mapped files require direct memory");

        File tmpfile = IOTools.createTempFile("sync.dat");
        try (MappedFile mappedFile = MappedFile.mappedFile(tmpfile, 64 << 20);
             MappedBytes bytes = MappedBytes.mappedBytes(mappedFile)) {
            mappedFile.syncMode(syncMode);
            bytes.readLong(0);
            MappedBytesStore mbs = uncheckedCast(bytes.bytesStore);
            assertEquals(syncMode,
                    mbs.syncMode(),
                    "Mapped bytes store should reflect the configured sync mode");
            for (int i = 0; i < 64 << 20; i += 1 << 20) {
                mbs.syncUpTo(i);
                for (int j = 0; j < 1 << 20; j += 4 << 10)
                    bytes.writeLong(i + j, j);
            }
        }
    }
}
