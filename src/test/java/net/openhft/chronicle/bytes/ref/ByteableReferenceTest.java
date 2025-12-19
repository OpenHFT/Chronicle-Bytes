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
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SuppressWarnings("rawtypes")
public class ByteableReferenceTest extends BytesTestCommon {

    private Supplier<AbstractReference> byteableCtor;

    public void initByteableReferenceTest(final String className, final Supplier<AbstractReference> byteableCtor) {
        this.byteableCtor = byteableCtor;
    }

    public static List<Object[]> testData() {
        List<Object[]> objects = Arrays.asList(
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

    private static Object[] datum(final Supplier<Byteable> reference) {
        return new Object[]{reference.getClass().getSimpleName(), reference};
    }

    @MethodSource("testData")
    @ParameterizedTest(name = "{0}")
    public void shouldMakeReservationOnCurrentStore(final String className, final Supplier<AbstractReference> byteableCtor) {
        initByteableReferenceTest(className, byteableCtor);
        final BytesStore<?, ?> firstStore = BytesStore.nativeStore(64);
        try {
            firstStore.writeLong(0, 17);
            final BytesStore<?, ?> secondStore = BytesStore.nativeStore(64);
            try (AbstractReference byteable = byteableCtor.get()) {
                secondStore.writeLong(0, 17);
                final long startCount = firstStore.refCount();
                byteable.bytesStore(firstStore, 0, byteable.maxSize());

                assertEquals(startCount + 1, firstStore.refCount(), "Byteable should reserve first store when attached");

                byteable.bytesStore(secondStore, 0, byteable.maxSize());

                assertEquals(startCount, firstStore.refCount(), "Byteable should release first store when attaching to second store");
            } finally {
                secondStore.releaseLast();
            }
        } finally {
            firstStore.releaseLast();
        }
    }
}
