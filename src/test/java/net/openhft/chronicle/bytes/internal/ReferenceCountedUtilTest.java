/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes.internal;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.bytes.BytesTestCommon;
import net.openhft.chronicle.core.io.ClosedIllegalStateException;
import net.openhft.chronicle.core.io.ReferenceCounted;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ReferenceCountedUtilTest extends BytesTestCommon {

    @Test
    @DisplayName("release guard throws on released reference object")
    void throwExceptionIfReleased() {
        test(o -> ReferenceCountedUtil.throwExceptionIfReleased((ReferenceCounted) o),
                "ReferenceCounted guard should detect released instances");
    }

    @Test
    @DisplayName("release guard accepts non-reference and rejects null")
    void testThrowExceptionIfReleased() {
        test(ReferenceCountedUtil::throwExceptionIfReleased,
                "Generic guard should detect released Bytes instances");
        assertDoesNotThrow(() -> ReferenceCountedUtil.throwExceptionIfReleased("Foo"),
                "Non-reference values should be accepted by the guard");
        assertThrows(NullPointerException.class,
                () -> ReferenceCountedUtil.throwExceptionIfReleased(null),
                "Null value should trigger NullPointerException in the guard");
    }

    private void test(Consumer<Object> method, String releasedMessage) {
        final Bytes<?> bytes = Bytes.from("A");
        assertDoesNotThrow(() -> method.accept(bytes),
                "Guard should accept a live Bytes instance");
        bytes.releaseLast();
        assertThrows(ClosedIllegalStateException.class,
                () -> method.accept(bytes),
                releasedMessage);
    }
}
