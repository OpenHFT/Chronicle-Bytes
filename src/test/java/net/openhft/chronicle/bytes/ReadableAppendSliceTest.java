/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes;

import static org.junit.Assert.*;

import org.junit.Test;

public class ReadableAppendSliceTest extends BytesTestCommon {

    private void append(boolean direct, boolean eightBit, boolean empty) {
        Bytes<?> source = Bytes.from("prefix:value");
        Bytes<?> destination =
                direct ? Bytes.allocateElasticDirect() : Bytes.allocateElasticOnHeap();
        try {
            source.readPosition(empty ? source.readLimit() : 7);
            long position = source.readPosition(), limit = source.readLimit();
            destination.append("before-");
            if (eightBit) destination.append8bit((BytesStore<?, ?>) source);
            else destination.appendUtf8((CharSequence) source);
            assertEquals(empty ? "before-" : "before-value", destination.toString());
            assertEquals(position, source.readPosition());
            assertEquals(limit, source.readLimit());
        } finally {
            destination.releaseLast();
            source.releaseLast();
        }
    }

    @Test
    public void directUtf8UsesReadableSlice() throws Exception {
        append(true, false, false);
    }

    @Test
    public void heapUtf8UsesReadableSlice() throws Exception {
        append(false, false, false);
    }

    @Test
    public void directEightBitUsesReadableSlice() throws Exception {
        append(true, true, false);
    }

    @Test
    public void heapEightBitUsesReadableSlice() throws Exception {
        append(false, true, false);
    }

    @Test
    public void emptyReadableSliceDoesNotCopyPrefix() throws Exception {
        append(true, false, true);
    }
}
