package net.openhft.chronicle.bytes.internal;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.bytes.BytesTestCommon;
import net.openhft.chronicle.core.io.ClosedIllegalStateException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ResourceUtilTest extends BytesTestCommon {

    @Test
    void throwExceptionIfReleased() {
        final Bytes<?> bytes = Bytes.from("A");
        assertDoesNotThrow(() -> ReferenceCountedUtil.throwExceptionIfReleased(bytes));
        bytes.releaseLast();
        assertThrows(ClosedIllegalStateException.class, () -> ReferenceCountedUtil.throwExceptionIfReleased(bytes));
    }
}