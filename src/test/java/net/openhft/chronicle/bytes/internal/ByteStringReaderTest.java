/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes.internal;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.bytes.BytesTestCommon;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests for ByteStringReader, because readers must handle closed state
 * gracefully to avoid exceptions during stream-based processing.
 */
@DisplayName("ByteStringReader handles closed state and skip behaviour")
@SuppressWarnings("PMD.JUnit5TestShouldBePackagePrivate")
class ByteStringReaderTest extends BytesTestCommon {

    @Test
    @DisplayName("read returns -1 when the backing parser is closed")
    public void readReturnsMinusOneWhenClosed() {
        Bytes<?> bytes = Bytes.from("a");
        try (ByteStringReader reader = new ByteStringReader(bytes)) {
            bytes.releaseLast();
            assertEquals(-1,
                    reader.read(),
                    "Closed readers should return -1 to signal end of stream");
        }
    }

    @Test
    @DisplayName("skip advances the reader and returns skipped count")
    public void skipAdvancesReader()
            throws java.io.IOException {
        Bytes<?> bytes = Bytes.from("abc");
        try {
            try (ByteStringReader reader = new ByteStringReader(bytes)) {
                assertEquals(2L,
                        reader.skip(2),
                        "skip should return the number of bytes skipped");
                assertEquals('c',
                        reader.read(),
                        "skip should advance the reader to the remaining character");
            }
        } finally {
            bytes.releaseLast();
        }
    }
}
