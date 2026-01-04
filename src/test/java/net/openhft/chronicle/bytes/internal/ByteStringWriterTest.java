/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes.internal;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.bytes.BytesTestCommon;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("ByteStringWriter handles closed state and error translation")
public class ByteStringWriterTest extends BytesTestCommon {

    @Test
    @DisplayName("write wraps IllegalStateException as IOException when closed")
    public void writeWrapsIllegalState() {
        Bytes<?> bytes = Bytes.allocateElasticOnHeap(8);
        try (ByteStringWriter writer = new ByteStringWriter(bytes)) {
            bytes.releaseLast();
            assertThrows(IOException.class,
                    () -> writer.write('a'),
                    "Closed writers should wrap IllegalStateException as IOException");
        }
    }
}
