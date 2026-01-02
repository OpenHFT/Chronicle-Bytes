/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DisplayName("BytesStore substring and slicing scenario coverage")
public class BytesStoreTest extends BytesTestCommon {
    @Test
    @DisplayName("Create BytesStore from short literal bytes")
    public void from() {
        BytesStore<?, byte[]> from = BytesStore.from(", ");
        assertEquals(2, from.capacity(), "Capacity should match literal length");
        from.releaseLast();
    }

    @Test
    @DisplayName("SubBytes from literal return expected slice text")
    public void from2() {
        Bytes<?> hello = Bytes.from("Hello").subBytes(0, 5).bytesForRead();
        assertEquals("Hello", hello.toString(), "Full slice should return Hello");

        Bytes<?> hell = Bytes.from("Hello").subBytes(0, 4).bytesForRead();
        assertEquals("Hell", hell.toString(), "Four-character slice should return Hell");

        Bytes<?> ell = Bytes.from("Hello").subBytes(1, 3).bytesForRead();
        assertEquals("ell", ell.toString(), "Middle slice should return ell");
    }

    @Test
    @DisplayName("Heap bytes subsequence returns expected slices")
    public void testSubSequenceOnHeap() {
        final Bytes<?> bytes = Bytes.allocateElasticOnHeap();

        bytes.append("Hello");

        testSubSequence(bytes);
    }

    @Test
    @DisplayName("Direct bytes subsequence returns expected slices")
    public void testSubSequenceDirect() {
        final Bytes<?> bytes = Bytes.allocateElasticDirect();

        bytes.append("Hello");

        testSubSequence(bytes);

        bytes.releaseLast();
    }

    private void testSubSequence(Bytes<?> hello) {
        CharSequence helloSubsequence = hello.subSequence(0, 5);
        assertEquals("Hello", helloSubsequence.toString(), "Full subsequence should return Hello");

        CharSequence hellSubsequence = hello.subSequence(0, 4);
        assertEquals("Hell", hellSubsequence.toString(), "Prefix subsequence should return Hell");

        CharSequence ellSubsequence = hello.subSequence(1, 4);
        assertEquals("ell", ellSubsequence.toString(), "Inner subsequence should return ell");

        CharSequence elSubsequence = hello.subSequence(1, 3);
        assertEquals("el", elSubsequence.toString(), "Short inner subsequence should return el");

        hello.readPosition(1);

        assertEquals("ello", hello.toString(), "Read position shift should expose ello");

        CharSequence elloSubsequence = hello.subSequence(0, 4);
        assertEquals("ello", elloSubsequence.toString(), "Subsequence after shift should return ello");

        CharSequence llSubsequence = hello.subSequence(1, 3);
        assertEquals("ll", llSubsequence.toString(), "Inner subsequence after shift should return ll");

        CharSequence loSubsequence = hello.subSequence(2, 4);
        assertEquals("lo", loSubsequence.toString(), "Tail subsequence after shift should return lo");

        assertEquals('l', hello.charAt(2), "charAt should return l at index 2");
        assertEquals('o', hello.charAt(3), "charAt should return o at index 3");

        CharSequence emptySubsequence = hello.subSequence(3, 3);
        assertEquals("", emptySubsequence.toString(), "Empty subsequence should return empty string");
    }
}
