/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static java.nio.charset.StandardCharsets.ISO_8859_1;
import static org.junit.jupiter.api.Assertions.assertEquals;

@DisplayName("Bytes indexOf covers empty input and offset branch scenarios")
@SuppressWarnings("PMD.JUnit5TestShouldBePackagePrivate")
class BytesIndexOfTest extends BytesTestCommon {

    @Test
    @DisplayName("indexOf handles empty source and empty target")
    public void indexOfHandlesEmptyInputs() {
        Bytes<?> empty = Bytes.wrapForRead(new byte[0]);
        Bytes<?> target = Bytes.wrapForRead("a".getBytes(ISO_8859_1));
        Bytes<?> emptyTarget = Bytes.wrapForRead(new byte[0]);
        try {
            assertEquals(-1,
                    empty.indexOf(target),
                    "Empty source should return -1 for non-empty target");
            assertEquals(0,
                    target.indexOf(emptyTarget),
                    "Non-empty source should return 0 for empty target");
        } finally {
            empty.releaseLast();
            target.releaseLast();
            emptyTarget.releaseLast();
        }
    }

    @Test
    @DisplayName("indexOf finds the first occurrence of a sub-sequence")
    public void indexOfFindsFirstOccurrence() {
        Bytes<?> source = Bytes.wrapForRead("abcabc".getBytes(ISO_8859_1));
        Bytes<?> target = Bytes.wrapForRead("cab".getBytes(ISO_8859_1));
        try {
            assertEquals(2,
                    source.indexOf(target),
                    "indexOf should locate the first occurrence of the sub-sequence");
        } finally {
            source.releaseLast();
            target.releaseLast();
        }
    }

    @Test
    @DisplayName("indexOf with fromIndex honours bounds and empty targets")
    public void indexOfWithFromIndexHandlesBounds() {
        Bytes<?> source = Bytes.wrapForRead("abcabc".getBytes(ISO_8859_1));
        BytesStore<?, ?> target = BytesStore.wrap("bc".getBytes(ISO_8859_1));
        BytesStore<?, ?> empty = BytesStore.wrap(new byte[0]);
        try {
            assertEquals(1,
                    source.indexOf(target, -1),
                    "Negative fromIndex should behave like zero");
            assertEquals(-1,
                    source.indexOf(target, 10),
                    "fromIndex beyond length should return -1 for non-empty target");
            assertEquals(6,
                    source.indexOf(empty, 6),
                    "fromIndex at length should return length for empty target");
        } finally {
            source.releaseLast();
            target.releaseLast();
            empty.releaseLast();
        }
    }
}
