/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes.ref;

import net.openhft.chronicle.bytes.Byteable;
import net.openhft.chronicle.bytes.BytesStore;
import net.openhft.chronicle.bytes.BytesTestCommon;
import net.openhft.chronicle.core.io.AbstractCloseable;
import net.openhft.chronicle.core.io.AbstractReferenceCounted;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.function.Supplier;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SuppressWarnings("rawtypes")
public class ByteableReferenceTest extends BytesTestCommon {

    public static Stream<Arguments> testData() {
        Stream<Arguments> objects = Stream.of(
                datum(BinaryLongReference::new),
                datum(BinaryTwoLongReference::new),
                datum(BinaryBooleanReference::new),
                datum(BinaryIntReference::new),
                datum(TextBooleanReference::new),
                datum(TextIntReference::new),
                datum(TextLongReference::new)
        );
        AbstractCloseable.disableCloseableTracing();
        AbstractReferenceCounted.disableReferenceTracing();
        return objects;
    }

    private static Arguments datum(final Supplier<Byteable> reference) {
        return Arguments.of(reference.getClass().getSimpleName(), reference);
    }

    @ParameterizedTest(name = "{0} reserves store")
    @DisplayName("byteable reference reserves and releases stores")
    @MethodSource("testData")
    public void shouldMakeReservationOnCurrentStore(String className, Supplier<AbstractReference> byteableCtor) {
        final BytesStore<?, ?> firstStore = BytesStore.nativeStore(64);
        try {
            firstStore.writeLong(0, 17);
            final BytesStore<?, ?> secondStore = BytesStore.nativeStore(64);
            try (AbstractReference byteable = byteableCtor.get()) {
                secondStore.writeLong(0, 17);
                final long startCount = firstStore.refCount();
                byteable.bytesStore(firstStore, 0, byteable.maxSize());

                assertEquals(startCount + 1,
                        firstStore.refCount(),
                        "Reference should reserve the first store for " + className);

                byteable.bytesStore(secondStore, 0, byteable.maxSize());

                assertEquals(startCount,
                        firstStore.refCount(),
                        "Reference should release the first store when swapped for " + className);
            } finally {
                secondStore.releaseLast();
            }
        } finally {
            firstStore.releaseLast();
        }
    }
}
