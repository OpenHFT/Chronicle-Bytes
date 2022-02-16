package net.openhft.chronicle.bytes.internal;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.bytes.BytesTestCommon;
import net.openhft.chronicle.core.io.ClosedIllegalStateException;
import net.openhft.chronicle.core.io.ReferenceCounted;
import org.junit.jupiter.api.Test;

import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ReferenceCountedUtilTest extends BytesTestCommon {

    @Test
    void throwExceptionIfReleased() {
        test(o -> ReferenceCountedUtil.throwExceptionIfReleased((ReferenceCounted) o));
    }

    @Test
    void testThrowExceptionIfReleased() {
        test(ReferenceCountedUtil::throwExceptionIfReleased);
        assertDoesNotThrow(() -> ReferenceCountedUtil.throwExceptionIfReleased("Foo"));
        assertThrows(NullPointerException.class, () -> ReferenceCountedUtil.throwExceptionIfReleased(null));
    }

    private void test(Consumer<Object> method) {
        final Bytes<?> bytes = Bytes.from("A");
        assertDoesNotThrow(() -> method.accept(bytes));
        bytes.releaseLast();
        assertThrows(ClosedIllegalStateException.class, () -> method.accept(bytes));
    }

}