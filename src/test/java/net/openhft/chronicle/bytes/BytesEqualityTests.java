/*
 * Copyright (c) 2016-2022 chronicle.software
 *
 *     https://chronicle.software
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

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

class BytesEqualityTests {

    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    @Nested
    class BoundaryTests {

        @ParameterizedTest
        @MethodSource("bufferArguments")
        void zeroLength(Bytes<?> left, Bytes<?> right) {
            assertEquals(left, right);
        }

        @ParameterizedTest
        @MethodSource("bufferArguments")
        void differentLength(Bytes<?> left, Bytes<?> right) {
            left.write("tex".getBytes());
            right.write("text".getBytes());
            assertNotEquals(left, right);
        }

        @ParameterizedTest
        @MethodSource("bufferArguments")
        void shortEqual(Bytes<?> left, Bytes<?> right) {
            left.write("abc".getBytes());
            right.write("abc".getBytes());
            assertEquals(left, right);
        }

        @ParameterizedTest
        @MethodSource("bufferArguments")
        void longEquals(Bytes<?> left, Bytes<?> right) {
            left.write("abcdefghijklmnopqrstuvwxyz".getBytes());
            right.write("abcdefghijklmnopqrstuvwxyz".getBytes());
            assertEquals(left, right);
        }

        @ParameterizedTest
        @MethodSource("bufferArguments")
        void longNotEquals(Bytes<?> left, Bytes<?> right) {
            left.write("abcdefghijklmnopqrstuvwxyz".getBytes());
            right.write("abcdefghijklmnopqrst_vwxyz".getBytes());
            assertNotEquals(left, right);
        }

        @ParameterizedTest
        @MethodSource("bufferArguments")
        void longEqualsBeforeSkip(Bytes<?> left, Bytes<?> right) {
            left.write("abcdefghijklmnopqrstuvwxyz".getBytes());
            left.readSkip(8);
            right.write("_bcdefghijklmnopqrstuvwxyz".getBytes());
            right.readPosition(8);
            assertEquals(left, right);
        }

        @ParameterizedTest
        @MethodSource("bufferArguments")
        void longNotEqualsAfterSkip(Bytes<?> left, Bytes<?> right) {
            left.write("abcdefghijklmnopqrstuvwxyz".getBytes());
            left.readSkip(8);
            right.write("abcdefghijklmnopqrstuvwxy_".getBytes());
            right.readPosition(8);
            assertNotEquals(left, right);
        }

        @ParameterizedTest
        @MethodSource("bufferArguments")
        void longEqualsWithReadSkip(Bytes<?> left, Bytes<?> right) {
            left.write("abcdefghijklmnopqrstuvwxyz".getBytes());
            left.readSkip(8);
            right.write("abcdefghijklmnopqrstuvwxyz".getBytes());
            right.readSkip(8);
            assertEquals(left, right);
        }

        public Stream<Arguments> bufferArguments() {
            return Stream.of(
                    Arguments.of(Bytes.allocateElasticOnHeap(), Bytes.allocateElasticDirect()), // heap, direct
                    Arguments.of(Bytes.allocateElasticDirect(), Bytes.allocateElasticOnHeap()), // direct, heap
                    Arguments.of(Bytes.allocateElasticDirect(), Bytes.allocateElasticDirect()), // direct, direct
                    Arguments.of(Bytes.allocateElasticOnHeap(), Bytes.allocateElasticOnHeap()) // heap, heap
            );
        }
    }

    /**
     * A suite of tests for exercising contentEquals and a variety of different heap and direct buffer combinations.
     */
    @Nested
    class DirectVsHeapContentEqualsTests {

        @Test
        void baseFailureCase() {
            byte[] source = "bazquux foobar plughfred".getBytes();
            Bytes<?> direct = Bytes.allocateElasticDirect();
            Bytes<?> heap = Bytes.allocateElasticOnHeap();
            direct.write(source);
            direct.readSkip(8);
            heap.write(source);
            heap.readSkip(8);
            assertEquals(direct, heap);
        }

        @Test
        void baseFailureCase_argumentsPermuted() {
            byte[] source = "bazquux foobar plughfred".getBytes();
            Bytes<?> direct = Bytes.allocateElasticDirect();
            Bytes<?> heap = Bytes.allocateElasticOnHeap();
            direct.write(source);
            direct.readSkip(8);
            heap.write(source);
            heap.readSkip(8);
            assertEquals(heap, direct);
        }

        @Test
        void baseFailureCase_fixedSizeDirectBuffer() {
            byte[] source = "bazquux foobar plughfred".getBytes();
            Bytes<?> direct = Bytes.allocateDirect(1024);
            Bytes<?> heap = Bytes.allocateElasticOnHeap();
            direct.write(source);
            direct.readSkip(8);
            heap.write(source);
            heap.readSkip(8);
            assertEquals(direct, heap);
        }

        @Test
        void baseCase_readSkip1() {
            byte[] source = "bazquux foobar plughfred".getBytes();
            Bytes<?> direct = Bytes.allocateElasticDirect();
            Bytes<?> heap = Bytes.allocateElasticOnHeap();
            direct.write(source);
            direct.readSkip(1);
            heap.write(source);
            heap.readSkip(1);
            assertEquals(direct, heap);
        }

        @Test
        void withoutReadSkip() {
            byte[] source = "bazquux foobar plughfred".getBytes();
            Bytes<?> direct = Bytes.allocateElasticDirect();
            Bytes<?> heap = Bytes.allocateElasticOnHeap();
            direct.write(source);
            heap.write(source);
            assertEquals(direct, heap);
        }

        @Test
        void directDirect_withSkip() {
            byte[] source = "bazquux foobar plughfred".getBytes();
            Bytes<?> direct1 = Bytes.allocateElasticDirect();
            Bytes<?> direct2 = Bytes.allocateElasticDirect();
            direct1.write(source);
            direct1.readSkip(8);
            direct2.write(source);
            direct2.readSkip(8);
            assertEquals(direct1, direct2);
        }

        @Test
        void directDirect_noSkip() {
            byte[] source = "bazquux foobar plughfred".getBytes();
            Bytes<?> direct1 = Bytes.allocateElasticDirect();
            Bytes<?> direct2 = Bytes.allocateElasticDirect();
            direct1.write(source);
            direct2.write(source);
            assertEquals(direct1, direct2);
        }

        @Test
        void heapHeap_withSkip() {
            byte[] source = "bazquux foobar plughfred".getBytes();
            Bytes<?> heap1 = Bytes.allocateElasticOnHeap();
            Bytes<?> heap2 = Bytes.allocateElasticOnHeap();
            heap1.write(source);
            heap1.readSkip(8);
            heap2.write(source);
            heap2.readSkip(8);
            assertEquals(heap1, heap2);
        }

        @Test
        void heapHeap_noSkip() {
            byte[] source = "bazquux foobar plughfred".getBytes();
            Bytes<?> heap1 = Bytes.allocateElasticOnHeap();
            Bytes<?> heap2 = Bytes.allocateElasticOnHeap();
            heap1.write(source);
            heap2.write(source);
            assertEquals(heap1, heap2);
        }

        @Test
        void shortString() {
            byte[] source = "test".getBytes();
            Bytes<?> direct = Bytes.allocateElasticDirect();
            Bytes<?> heap = Bytes.allocateElasticOnHeap();

            direct.write(source);
            direct.readSkip(1);

            heap.write(source);
            heap.readSkip(1);

            assertEquals(direct, heap);
        }
    }
}
