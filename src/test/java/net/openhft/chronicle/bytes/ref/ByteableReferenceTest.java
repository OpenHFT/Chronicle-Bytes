package net.openhft.chronicle.bytes.ref;

import net.openhft.chronicle.bytes.Byteable;
import net.openhft.chronicle.bytes.NativeBytesStore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

@SuppressWarnings({"rawtypes", "unchecked"})
@RunWith(Parameterized.class)
public class ByteableReferenceTest {
    private final Byteable byteable;

    public ByteableReferenceTest(final String className, final Byteable byteable) {
        this.byteable = byteable;
    }

    @Parameterized.Parameters(name = "{0}")
    public static List<Object[]> testData() {
        return Arrays.asList(
                datum(new BinaryLongReference()),
                datum(new BinaryTwoLongReference()),
                datum(new BinaryBooleanReference()),
                datum(new BinaryIntReference()),
                datum(new TextBooleanReference()),
                datum(new TextIntReference()),
                datum(new TextLongReference())
                /*,
                unhelpful implementations below this point
                datum(new TextLongArrayReference()),
                datum(new BinaryLongArrayReference()),
                datum(new UncheckedLongReference())*/
        );
    }

    private static Object[] datum(final Byteable reference) {
        return new Object[]{reference.getClass().getSimpleName(), reference};
    }

    @Test
    public void shouldMakeReservationOnCurrentStore() {
        final NativeBytesStore<Void> firstStore = NativeBytesStore.nativeStore(64);
        firstStore.writeLong(0, 17);
        final NativeBytesStore<Void> secondStore = NativeBytesStore.nativeStore(64);
        secondStore.writeLong(0, 17);
        final long startCount = firstStore.refCount();
        byteable.bytesStore(firstStore, 0, byteable.maxSize());

        assertThat(firstStore.refCount(), is(startCount + 1));

        byteable.bytesStore(secondStore, 0, byteable.maxSize());

        assertThat(firstStore.refCount(), is(startCount));
    }

}