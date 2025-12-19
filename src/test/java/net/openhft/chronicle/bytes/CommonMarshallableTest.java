/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class CommonMarshallableTest extends BytesTestCommon {

    @Test
    public void usesSelfDescribingMessage() {
        assertTrue(new CommonMarshallable() {
        }.usesSelfDescribingMessage(), "usesSelfDescribingMessage: assertTrue");
    }
}
