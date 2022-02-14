package net.openhft.chronicle.bytes.ref;

import net.openhft.chronicle.bytes.Byteable;
import net.openhft.chronicle.bytes.BytesStore;
import net.openhft.chronicle.bytes.BytesTestCommon;
import net.openhft.chronicle.core.io.AbstractCloseable;
import net.openhft.chronicle.core.io.AbstractReferenceCounted;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;

import static org.junit.Assert.assertEquals;

@SuppressWarnings({"rawtypes", "unchecked"})
@RunWith(Parameterized.class)
public class ByteableReferenceTest extends BytesTestCommon {

    private final Supplier<AbstractReference> byteableCtor;

    public ByteableReferenceTest(final String className, final Supplier<AbstractReference> byteableCtor) {
        this.byteableCtor = byteableCtor;
    }

    @Parameterized.Parameters(name = "{0}")
    public static List<Object[]> testData() {
        List<Object[]> objects = Arrays.asList(
                datum(BinaryLongReference::new),
                datum(BinaryTwoLongReference::new),
                datum(BinaryBooleanReference::new),
                datum(BinaryIntReference::new),
                datum(TextBooleanReference::new),
                datum(TextIntReference::new),
                datum(TextLongReference::new)
                /*,
                unhelpful implementations below this point
                datum(new TextLongArrayReference()),
                datum(new BinaryLongArrayReference()),
                datum(new UncheckedLongReference())*/
        );
        AbstractCloseable.disableCloseableTracing();
        AbstractReferenceCounted.disableReferenceTracing();
        return objects;
    }

    private static Object[] datum(final Supplier<Byteable> reference) {
        return new Object[]{reference.getClass().getSimpleName(), reference};
    }

    @Test
    public void shouldMakeReservationOnCurrentStore() {
        final BytesStore firstStore = BytesStore.nativeStore(64);
        try {
            firstStore.writeLong(0, 17);
            final BytesStore secondStore = BytesStore.nativeStore(64);
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