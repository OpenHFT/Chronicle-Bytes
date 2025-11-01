/*
 * Copyright 2016-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes;

import net.openhft.chronicle.core.io.InvalidMarshallableException;

interface IBytesMethod {
    @MethodId(0x81L)
    void myByteable(MyByteable byteable) throws InvalidMarshallableException;

    @MethodId(0x82L)
    void myScalars(MyScalars scalars);

    @MethodId(0x83L)
    void myNested(MyNested nested);
}
