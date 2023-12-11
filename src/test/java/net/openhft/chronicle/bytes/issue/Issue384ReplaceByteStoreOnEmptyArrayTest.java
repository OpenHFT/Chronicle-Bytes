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
package net.openhft.chronicle.bytes.issue;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.bytes.BytesTestCommon;
import net.openhft.chronicle.bytes.VanillaBytes;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.nio.BufferOverflowException;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;

final class Issue384ReplaceByteStoreOnEmptyArrayTest extends BytesTestCommon {

    @ParameterizedTest(name = "{index}: ({0})")
    @MethodSource("bytesToTest")
    void reproduce(String classSimpleName, Bytes<?> bytes) {

        // throwing AssertionErrors in the code makes testing more complicated.
        boolean replacedOrRefused = false;
        try {
            // Write an empty array into the empty bytes. This should trigger a
            // resize since the empty bytes is using a shared backing EmptyByteStore.
            bytes.ensureCapacity(1);
            // Make sure we are not using an EmptyByteStore
            replacedOrRefused = bytes.capacity() != 0;
        } catch (BufferOverflowException ignored) {
            // This is ok as some of the Bytes objects are not elastic.
            replacedOrRefused = true;
        } finally {
            bytes.releaseLast();
        }
        assertTrue(replacedOrRefused);
    }

    static Stream<Arguments> bytesToTest() {
        return Stream.of(
                        Bytes.allocateElasticDirect(),
                        Bytes.allocateElasticDirect(0),
                        Bytes.allocateDirect(0),
                        Bytes.allocateElasticOnHeap(0),
                        VanillaBytes.vanillaBytes(),
                        Bytes.from(""),
                        Bytes.allocateDirect(new byte[0]),
                        Bytes.fromDirect("")
                )
                .map(b -> Arguments.of(b.getClass().getSimpleName() + (b.isElastic() ? " Elastic" : " Fixed"), b));
    }

}
