/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes.issue;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.bytes.BytesTestCommon;
import net.openhft.chronicle.bytes.VanillaBytes;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.nio.BufferOverflowException;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Regression tests for Issue 384, because writing to empty Bytes should
 * trigger a backing store replacement or refuse the operation, to avoid
 * corrupting the shared empty singleton.
 */
@DisplayName("Issue 384 - Empty array backing store replacement validation")
final class Issue384ReplaceByteStoreOnEmptyArrayTest extends BytesTestCommon {

    @ParameterizedTest(name = "{index}: ({0})")
    @DisplayName("empty arrays replace backing byte store as needed")
    @MethodSource("bytesToTest")
    void reproduce(String classSimpleName, Bytes<?> bytes) {

        // throwing AssertionErrors in the code makes testing more complicated.
        boolean replacedOrRefused;
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
        assertTrue(replacedOrRefused,
                "Empty bytes should replace the backing store or refuse growth for " + classSimpleName);
    }

    private static Stream<Arguments> bytesToTest() {
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
