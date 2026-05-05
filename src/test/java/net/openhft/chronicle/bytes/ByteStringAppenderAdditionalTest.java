/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests additional ByteStringAppender branches for append operations because complete coverage
 * of formatting paths is essential for reliable string serialisation.
 */
@SuppressWarnings({"deprecation", "PMD.JUnit5TestShouldBePackagePrivate"})
@DisplayName("ByteStringAppender covers append branches and formatting paths")
class ByteStringAppenderAdditionalTest extends BytesTestCommon {

    @Test
    @DisplayName("append handles empty sequences and base conversions")
    public void appendHandlesEmptyAndBaseConversions() {
        Bytes<?> bytes = Bytes.allocateElasticOnHeap(32);
        try {
            bytes.append("");
            bytes.appendBase(12, 10).append(',');
            bytes.appendBase(15, 16);
            assertEquals("12,f",
                    bytes.toString(),
                    "appendBase should format base 10 and base 16 values");
        } finally {
            bytes.releaseLast();
        }
    }

    @Test
    @DisplayName("append long dispatches int and long paths")
    public void appendLongDispatchesByRange() {
        Bytes<?> bytes = Bytes.allocateElasticOnHeap(64);
        try {
            long large = (long) Integer.MAX_VALUE + 1L;
            bytes.append(1L).append(',').append(large);
            assertEquals("1," + large,
                    bytes.toString(),
                    "append(long) should handle both int-range and long values");
        } finally {
            bytes.releaseLast();
        }
    }

    @Test
    @DisplayName("append8bit replaces non Latin-1 characters with placeholders")
    public void append8bitReplacesNonLatin1Characters() {
        Bytes<?> bytes = Bytes.allocateElasticOnHeap(32);
        try {
            String value = "A\u20ACB";
            bytes.append8bit(value);
            assertEquals("A?B",
                    bytes.toString(),
                    "append8bit should replace characters outside Latin-1 with '?'");
        } finally {
            bytes.releaseLast();
        }
    }

    @Test
    @DisplayName("append8bit accepts BytesStore-backed CharSequence ranges")
    public void append8bitUsesBytesStoreRanges() {
        Bytes<?> target = Bytes.allocateElasticOnHeap(32);
        Bytes<?> source = Bytes.from("wxyz");
        try {
            target.append8bit((CharSequence) source, 1, 3);
            assertEquals("xy",
                    target.toString(),
                    "append8bit should copy ranges from BytesStore-backed sequences");
        } finally {
            source.releaseLast();
            target.releaseLast();
        }
    }

    @Test
    @DisplayName("append boolean and non-empty sequences render expected output")
    public void appendBooleanAndNonEmptySequence() {
        Bytes<?> bytes = Bytes.allocateElasticOnHeap(32);
        try {
            bytes.append(true).append(',').append(false).append(',').append("ok");
            assertEquals("T,F,ok",
                    bytes.toString(),
                    "append should format booleans and append non-empty sequences");
        } finally {
            bytes.releaseLast();
        }
    }

    @Test
    @DisplayName("appendDecimal formats scaled values with decimal places")
    public void appendDecimalFormatsScaledValues() {
        Bytes<?> bytes = Bytes.allocateElasticOnHeap(32);
        try {
            bytes.appendDecimal(12345, 2);
            assertEquals("123.45",
                    bytes.toString(),
                    "appendDecimal should insert the decimal point at the expected position");
        } finally {
            bytes.releaseLast();
        }
    }
}
