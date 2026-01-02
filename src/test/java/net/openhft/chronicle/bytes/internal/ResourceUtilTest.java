/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes.internal;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.bytes.BytesTestCommon;
import net.openhft.chronicle.core.io.ClosedIllegalStateException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ResourceUtilTest extends BytesTestCommon {

    @Test
    @DisplayName("release guard throws after bytes resource is released")
    void throwExceptionIfReleased() {
        final Bytes<?> bytes = Bytes.from("A");
        assertDoesNotThrow(() -> ReferenceCountedUtil.throwExceptionIfReleased(bytes),
                "Guard should allow checks on a live Bytes resource");
        bytes.releaseLast();
        assertThrows(ClosedIllegalStateException.class,
                () -> ReferenceCountedUtil.throwExceptionIfReleased(bytes),
                "Guard should reject a released Bytes resource");
    }
}
