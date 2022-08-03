/*
 * Copyright (c) 2016-2022 chronicle.software
 *
 *       https://chronicle.software
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.openhft.chronicle.bytes;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class BytesStoreTest extends BytesTestCommon {
    @Test
    public void from() {
        BytesStore from = BytesStore.from(", ");
        assertEquals(2, from.capacity());
        from.releaseLast();
    }

    @Test
    public void from2() {
        Bytes<?> hello = Bytes.from("Hello").subBytes(0, 5).bytesForRead();
        assertEquals("Hello", hello.toString());

        Bytes<?> hell = Bytes.from("Hello").subBytes(0, 4).bytesForRead();
        assertEquals("Hell", hell.toString());

        Bytes<?> ell = Bytes.from("Hello").subBytes(1, 3).bytesForRead();
        assertEquals("ell", ell.toString());
    }

    @Test
    public void testSubSequenceOnHeap() {
        final Bytes<?> bytes = Bytes.allocateElasticOnHeap();

        bytes.append("Hello");

        testSubSequence(bytes);
    }

    @Test
    public void testSubSequenceDirect() {
        final Bytes<?> bytes = Bytes.allocateElasticDirect();

        bytes.append("Hello");

        testSubSequence(bytes);

        bytes.releaseLast();
    }

    private void testSubSequence(Bytes<?> hello) {
        CharSequence helloSubsequence = hello.subSequence(0, 5);
        assertEquals("Hello", helloSubsequence.toString());

        CharSequence hellSubsequence = hello.subSequence(0, 4);
        assertEquals("Hell", hellSubsequence.toString());

        CharSequence ellSubsequence = hello.subSequence(1, 4);
        assertEquals("ell", ellSubsequence.toString());

        CharSequence elSubsequence = hello.subSequence(1, 3);
        assertEquals("el", elSubsequence.toString());

        hello.readPosition(1);

        assertEquals("ello", hello.toString());

        CharSequence elloSubsequence = hello.subSequence(0, 4);
        assertEquals("ello", elloSubsequence.toString());

        CharSequence llSubsequence = hello.subSequence(1, 3);
        assertEquals("ll", llSubsequence.toString());

        CharSequence loSubsequence = hello.subSequence(2, 4);
        assertEquals("lo", loSubsequence.toString());

        assertEquals('l', hello.charAt(2));
        assertEquals('o', hello.charAt(3));

        CharSequence emptySubsequence = hello.subSequence(3, 3);
        assertEquals("", emptySubsequence.toString());
    }
}
