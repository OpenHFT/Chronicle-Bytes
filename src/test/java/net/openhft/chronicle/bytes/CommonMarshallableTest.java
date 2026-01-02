/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class CommonMarshallableTest extends BytesTestCommon {

    @Test
    @DisplayName("common marshallable uses self describing messages")
    public void usesSelfDescribingMessage() {
        assertTrue(new CommonMarshallable() {
        }.usesSelfDescribingMessage(),
                "CommonMarshallable defaults to self describing message");
    }
}
