/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class BytesStoreTest extends BytesTestCommon {
    @Test
    public void from() {
        BytesStore<?, byte[]> from = BytesStore.from(", ");
        assertEquals(2, from.capacity(), "BytesStore from ', ' should have capacity 2");
        from.releaseLast();
    }

    @Test
    public void from2() {
        Bytes<?> hello = Bytes.from("Hello").subBytes(0, 5).bytesForRead();
        assertEquals("Hello", hello.toString(), "subBytes(0,5) should produce 'Hello'");

        Bytes<?> hell = Bytes.from("Hello").subBytes(0, 4).bytesForRead();
        assertEquals("Hell", hell.toString(), "subBytes(0,4) should produce 'Hell'");

        Bytes<?> ell = Bytes.from("Hello").subBytes(1, 3).bytesForRead();
        assertEquals("ell", ell.toString(), "subBytes(1,3) should produce 'ell'");
    }

    @Test
    public void testSubSequenceOnHeap() {
        final Bytes<?> bytes = Bytes.allocateElasticOnHeap();

        bytes.append("Hello");
        assertEquals("Hello", bytes.toString(), "testSubSequenceOnHeap: initial bytes");

        testSubSequence(bytes);
    }

    @Test
    public void testSubSequenceDirect() {
        final Bytes<?> bytes = Bytes.allocateElasticDirect();

        bytes.append("Hello");
        assertEquals("Hello", bytes.toString(), "testSubSequenceDirect: initial bytes");

        testSubSequence(bytes);

        bytes.releaseLast();
    }

    private void testSubSequence(Bytes<?> hello) {
        CharSequence helloSubsequence = hello.subSequence(0, 5);
        assertEquals("Hello", helloSubsequence.toString(), "subSequence(0,5) should produce 'Hello'");

        CharSequence hellSubsequence = hello.subSequence(0, 4);
        assertEquals("Hell", hellSubsequence.toString(), "subSequence(0,4) should produce 'Hell'");

        CharSequence ellSubsequence = hello.subSequence(1, 4);
        assertEquals("ell", ellSubsequence.toString(), "subSequence(1,4) should produce 'ell'");

        CharSequence elSubsequence = hello.subSequence(1, 3);
        assertEquals("el", elSubsequence.toString(), "subSequence(1,3) should produce 'el'");

        hello.readPosition(1);

        assertEquals("ello", hello.toString(), "bytes after readPosition(1) should be 'ello'");

        CharSequence elloSubsequence = hello.subSequence(0, 4);
        assertEquals("ello", elloSubsequence.toString(), "subSequence(0,4) after readPosition(1) should produce 'ello'");

        CharSequence llSubsequence = hello.subSequence(1, 3);
        assertEquals("ll", llSubsequence.toString(), "subSequence(1,3) after readPosition(1) should produce 'll'");

        CharSequence loSubsequence = hello.subSequence(2, 4);
        assertEquals("lo", loSubsequence.toString(), "subSequence(2,4) after readPosition(1) should produce 'lo'");

        assertEquals('l', hello.charAt(2), "charAt(2) should return 'l' after readPosition(1)");
        assertEquals('o', hello.charAt(3), "charAt(3) should return 'o' after readPosition(1)");

        CharSequence emptySubsequence = hello.subSequence(3, 3);
        assertEquals("", emptySubsequence.toString(), "subSequence(3,3) should produce empty string");
    }
}
