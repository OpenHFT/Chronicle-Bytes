/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes.ref;

import net.openhft.chronicle.bytes.Byteable;
import net.openhft.chronicle.bytes.BytesStore;
import net.openhft.chronicle.bytes.BytesTestCommon;
import net.openhft.chronicle.core.io.AbstractCloseable;
import net.openhft.chronicle.core.io.AbstractReferenceCounted;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.function.Supplier;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

@SuppressWarnings("rawtypes")
class ByteableReferenceTest extends BytesTestCommon {

    static {
        AbstractCloseable.disableCloseableTracing();
        AbstractReferenceCounted.disableReferenceTracing();
    }

    static Stream<Arguments> testData() {
        return Stream.of(
                datum(BinaryLongReference::new),
                datum(BinaryTwoLongReference::new),
                datum(BinaryBooleanReference::new),
                datum(BinaryIntReference::new),
                datum(TextBooleanReference::new),
                datum(TextIntReference::new),
                datum(TextLongReference::new)
        );
    }

    private static Arguments datum(final Supplier<Byteable> reference) {
        return Arguments.of(reference.getClass().getSimpleName(), reference);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("testData")
    void shouldMakeReservationOnCurrentStore(String className, Supplier<AbstractReference> byteableCtor) {
        final BytesStore<?, ?> firstStore = BytesStore.nativeStore(64);
        try {
            firstStore.writeLong(0, 17);
            final BytesStore<?, ?> secondStore = BytesStore.nativeStore(64);
            try (AbstractReference byteable = byteableCtor.get()) {
                secondStore.writeLong(0, 17);
                final long startCount = firstStore.refCount();
                byteable.bytesStore(firstStore, 0, byteable.maxSize());

                assertEquals(startCount + 1, firstStore.refCount());

                byteable.bytesStore(secondStore, 0, byteable.maxSize());

                assertEquals(startCount, firstStore.refCount());
            } finally {
                secondStore.releaseLast();
            }
        } finally {
            firstStore.releaseLast();
        }
    }
}
