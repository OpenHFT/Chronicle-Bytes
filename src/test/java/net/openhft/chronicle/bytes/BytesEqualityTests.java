/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.nio.charset.StandardCharsets;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

/**
 * Tests equals and contentEquals across heap and direct buffers because correct
 * comparison is required to avoid data mismatches in caching and deduplication.
 */
@SuppressWarnings("checkstyle:MMOverusedWord")
class BytesEqualityTests {

    /**
     * Tests boundary conditions for Bytes equality because edge cases with zero length,
     * mismatched lengths, and aligned read positions must be handled correctly.
     */
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    @Nested
    @DisplayName("BoundaryTests - equality across zero, short, and long content lengths")
    class BoundaryTests {

        @ParameterizedTest
        @DisplayName("zero length buffers compare equal for empty content")
        @MethodSource("bufferArguments")
        void zeroLength(Bytes<?> left, Bytes<?> right) {
            assertEquals(left, right,
                    "zero length content should be equal for " + left.getClass().getSimpleName()
                            + " and " + right.getClass().getSimpleName());
        }

        @ParameterizedTest
        @DisplayName("different length buffers compare as not equal")
        @MethodSource("bufferArguments")
        void differentLength(Bytes<?> left, Bytes<?> right) {
            left.write("tex".getBytes(StandardCharsets.ISO_8859_1));
            right.write("text".getBytes(StandardCharsets.ISO_8859_1));
            assertNotEquals(left, right,
                    "different length content should not be equal for " + left.getClass().getSimpleName()
                            + " and " + right.getClass().getSimpleName());
        }

        @ParameterizedTest
        @DisplayName("short buffer content compares equal for matching text")
        @MethodSource("bufferArguments")
        void shortEqual(Bytes<?> left, Bytes<?> right) {
            left.write("abc".getBytes(StandardCharsets.ISO_8859_1));
            right.write("abc".getBytes(StandardCharsets.ISO_8859_1));
            assertEquals(left, right,
                    "short content should be equal for " + left.getClass().getSimpleName()
                            + " and " + right.getClass().getSimpleName());
        }

        @ParameterizedTest
        @DisplayName("long buffer content compares equal for matching text")
        @MethodSource("bufferArguments")
        void longEquals(Bytes<?> left, Bytes<?> right) {
            left.write("abcdefghijklmnopqrstuvwxyz".getBytes(StandardCharsets.ISO_8859_1));
            right.write("abcdefghijklmnopqrstuvwxyz".getBytes(StandardCharsets.ISO_8859_1));
            assertEquals(left, right,
                    "long content should be equal for " + left.getClass().getSimpleName()
                            + " and " + right.getClass().getSimpleName());
        }

        @ParameterizedTest
        @DisplayName("long buffer with a single mismatch compares not equal")
        @MethodSource("bufferArguments")
        void longNotEquals(Bytes<?> left, Bytes<?> right) {
            left.write("abcdefghijklmnopqrstuvwxyz".getBytes(StandardCharsets.ISO_8859_1));
            right.write("abcdefghijklmnopqrst_vwxyz".getBytes(StandardCharsets.ISO_8859_1));
            assertNotEquals(left, right,
                    "long content should differ for " + left.getClass().getSimpleName()
                            + " and " + right.getClass().getSimpleName());
        }

        @ParameterizedTest
        @DisplayName("aligned read positions compare equal after readSkip")
        @MethodSource("bufferArguments")
        void longEqualsBeforeSkip(Bytes<?> left, Bytes<?> right) {
            left.write("abcdefghijklmnopqrstuvwxyz".getBytes(StandardCharsets.ISO_8859_1));
            left.readSkip(8);
            right.write("_bcdefghijklmnopqrstuvwxyz".getBytes(StandardCharsets.ISO_8859_1));
            right.readPosition(8);
            assertEquals(left, right,
                    "aligned read positions should compare equal for " + left.getClass().getSimpleName()
                            + " and " + right.getClass().getSimpleName());
        }

        @ParameterizedTest
        @DisplayName("misaligned read positions compare not equal")
        @MethodSource("bufferArguments")
        void longNotEqualsAfterSkip(Bytes<?> left, Bytes<?> right) {
            left.write("abcdefghijklmnopqrstuvwxyz".getBytes(StandardCharsets.ISO_8859_1));
            left.readSkip(8);
            right.write("abcdefghijklmnopqrstuvwxy_".getBytes(StandardCharsets.ISO_8859_1));
            right.readPosition(8);
            assertNotEquals(left, right,
                    "misaligned read positions should differ for " + left.getClass().getSimpleName()
                            + " and " + right.getClass().getSimpleName());
        }

        @ParameterizedTest
        @DisplayName("matching content after readSkip compares equal")
        @MethodSource("bufferArguments")
        void longEqualsWithReadSkip(Bytes<?> left, Bytes<?> right) {
            left.write("abcdefghijklmnopqrstuvwxyz".getBytes(StandardCharsets.ISO_8859_1));
            left.readSkip(8);
            right.write("abcdefghijklmnopqrstuvwxyz".getBytes(StandardCharsets.ISO_8859_1));
            right.readSkip(8);
            assertEquals(left, right,
                    "matching content after readSkip should be equal for " + left.getClass().getSimpleName()
                            + " and " + right.getClass().getSimpleName());
        }

        Stream<Arguments> bufferArguments() {
            return Stream.of(
                    Arguments.of(Bytes.allocateElasticOnHeap(), Bytes.allocateElasticDirect()), // heap, direct
                    Arguments.of(Bytes.allocateElasticDirect(), Bytes.allocateElasticOnHeap()), // direct, heap
                    Arguments.of(Bytes.allocateElasticDirect(), Bytes.allocateElasticDirect()), // direct, direct
                    Arguments.of(Bytes.allocateElasticOnHeap(), Bytes.allocateElasticOnHeap()) // heap, heap
            );
        }
    }

    /**
     * Tests contentEquals for direct versus heap buffer combinations because correct
     * cross-memory comparison is required for interoperability between buffer types.
     */
    @SuppressWarnings("java:S5976")
    @Nested
    @DisplayName("DirectVsHeapContentEqualsTests - contentEquals across memory types")
    class DirectVsHeapContentEqualsTests {

        @Test
        @DisplayName("direct and heap buffers match after readSkip(8) on same source")
        void baseFailureCase() {
            byte[] source = "bazquux foobar plughfred".getBytes(StandardCharsets.ISO_8859_1);
            Bytes<?> direct = Bytes.allocateElasticDirect();
            Bytes<?> heap = Bytes.allocateElasticOnHeap();
            direct.write(source);
            direct.readSkip(8);
            heap.write(source);
            heap.readSkip(8);
            assertEquals(direct, heap,
                    "direct and heap content should match after readSkip(8) on same source");
        }

        @Test
        @DisplayName("heap and direct buffers match after readSkip(8) with swapped arguments")
        void baseFailureCase_argumentsPermuted() {
            byte[] source = "bazquux foobar plughfred".getBytes(StandardCharsets.ISO_8859_1);
            Bytes<?> direct = Bytes.allocateElasticDirect();
            Bytes<?> heap = Bytes.allocateElasticOnHeap();
            direct.write(source);
            direct.readSkip(8);
            heap.write(source);
            heap.readSkip(8);
            assertEquals(heap, direct,
                    "heap and direct content should match after readSkip(8) on same source");
        }

        @Test
        @DisplayName("fixed direct buffer matches heap after readSkip(8)")
        void baseFailureCase_fixedSizeDirectBuffer() {
            byte[] source = "bazquux foobar plughfred".getBytes(StandardCharsets.ISO_8859_1);
            Bytes<?> direct = Bytes.allocateDirect(1024);
            Bytes<?> heap = Bytes.allocateElasticOnHeap();
            direct.write(source);
            direct.readSkip(8);
            heap.write(source);
            heap.readSkip(8);
            assertEquals(direct, heap,
                    "fixed-size direct buffer should match heap after readSkip(8)");
        }

        @Test
        @DisplayName("readSkip(1) yields matching content in direct and heap buffers")
        void baseCase_readSkip1() {
            byte[] source = "bazquux foobar plughfred".getBytes(StandardCharsets.ISO_8859_1);
            Bytes<?> direct = Bytes.allocateElasticDirect();
            Bytes<?> heap = Bytes.allocateElasticOnHeap();
            direct.write(source);
            direct.readSkip(1);
            heap.write(source);
            heap.readSkip(1);
            assertEquals(direct, heap,
                    "direct and heap content should match after readSkip(1)");
        }

        @Test
        @DisplayName("buffers without readSkip match using direct and heap stores")
        void withoutReadSkip() {
            byte[] source = "bazquux foobar plughfred".getBytes(StandardCharsets.ISO_8859_1);
            Bytes<?> direct = Bytes.allocateElasticDirect();
            Bytes<?> heap = Bytes.allocateElasticOnHeap();
            direct.write(source);
            heap.write(source);
            assertEquals(direct, heap,
                    "direct and heap content should match with no readSkip applied");
        }

        @Test
        @DisplayName("direct buffers match after readSkip(8)")
        void directDirect_withSkip() {
            byte[] source = "bazquux foobar plughfred".getBytes(StandardCharsets.ISO_8859_1);
            Bytes<?> direct1 = Bytes.allocateElasticDirect();
            Bytes<?> direct2 = Bytes.allocateElasticDirect();
            direct1.write(source);
            direct1.readSkip(8);
            direct2.write(source);
            direct2.readSkip(8);
            assertEquals(direct1, direct2,
                    "two direct buffers should match after readSkip(8)");
        }

        @Test
        @DisplayName("direct buffers match with no readSkip applied")
        void directDirect_noSkip() {
            byte[] source = "bazquux foobar plughfred".getBytes(StandardCharsets.ISO_8859_1);
            Bytes<?> direct1 = Bytes.allocateElasticDirect();
            Bytes<?> direct2 = Bytes.allocateElasticDirect();
            direct1.write(source);
            direct2.write(source);
            assertEquals(direct1, direct2,
                    "two direct buffers should match with no readSkip applied");
        }

        @Test
        @DisplayName("heap buffers match after readSkip(8)")
        void heapHeap_withSkip() {
            byte[] source = "bazquux foobar plughfred".getBytes(StandardCharsets.ISO_8859_1);
            Bytes<?> heap1 = Bytes.allocateElasticOnHeap();
            Bytes<?> heap2 = Bytes.allocateElasticOnHeap();
            heap1.write(source);
            heap1.readSkip(8);
            heap2.write(source);
            heap2.readSkip(8);
            assertEquals(heap1, heap2,
                    "two heap buffers should match after readSkip(8)");
        }

        @Test
        @DisplayName("heap buffers match with no readSkip applied")
        void heapHeap_noSkip() {
            byte[] source = "bazquux foobar plughfred".getBytes(StandardCharsets.ISO_8859_1);
            Bytes<?> heap1 = Bytes.allocateElasticOnHeap();
            Bytes<?> heap2 = Bytes.allocateElasticOnHeap();
            heap1.write(source);
            heap2.write(source);
            assertEquals(heap1, heap2,
                    "two heap buffers should match with no readSkip applied");
        }

        @Test
        @DisplayName("short string matches across direct and heap after readSkip(1)")
        void shortString() {
            byte[] source = "test".getBytes(StandardCharsets.ISO_8859_1);
            Bytes<?> direct = Bytes.allocateElasticDirect();
            Bytes<?> heap = Bytes.allocateElasticOnHeap();

            direct.write(source);
            direct.readSkip(1);

            heap.write(source);
            heap.readSkip(1);

            assertEquals(direct, heap,
                    "short string content should match after readSkip(1) across buffer types");
        }
    }
}
