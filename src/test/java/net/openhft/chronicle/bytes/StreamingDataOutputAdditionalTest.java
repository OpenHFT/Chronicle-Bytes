/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SuppressWarnings("deprecation")
@DisplayName("StreamingDataOutput covers decimal stop bit encoding branches")
public class StreamingDataOutputAdditionalTest extends BytesTestCommon {

    @Test
    @DisplayName("writeStopBitDecimal encodes whole numbers with scale zero")
    public void writeStopBitDecimalWholeNumber() {
        Bytes<?> bytes = Bytes.allocateElasticOnHeap(32);
        try {
            bytes.writeStopBitDecimal(12.0);
            bytes.readPosition(0);
            assertEquals(120L,
                    bytes.readStopBit(),
                    "Whole numbers should encode with scale zero");
        } finally {
            bytes.releaseLast();
        }
    }

    @Test
    @DisplayName("writeStopBitDecimal encodes fractional numbers with scale")
    public void writeStopBitDecimalFractional() {
        Bytes<?> bytes = Bytes.allocateElasticOnHeap(32);
        try {
            bytes.writeStopBitDecimal(-12.5);
            bytes.readPosition(0);
            assertEquals(-1251L,
                    bytes.readStopBit(),
                    "Fractional values should encode mantissa and scale");
        } finally {
            bytes.releaseLast();
        }
    }
}
