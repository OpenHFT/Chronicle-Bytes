/*
 * Copyright 2016-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes;

import org.junit.Test;

import static org.junit.Assert.assertTrue;

public class CommonMarshallableTest extends BytesTestCommon {

    @Test
    public void usesSelfDescribingMessage() {
        assertTrue(new CommonMarshallable() {
        }.usesSelfDescribingMessage());
    }
}
