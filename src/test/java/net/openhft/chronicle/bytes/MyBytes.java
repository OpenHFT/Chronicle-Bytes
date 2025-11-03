/*
 * Copyright 2016-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes;

import java.io.Closeable;
import java.io.IOException;

class MyBytes implements BytesMarshallable, Closeable {
    private Bytes<?> bytes1;
    private Bytes<?> bytes2;

    public MyBytes() {
    }

    public MyBytes(Bytes<?> bytes1, Bytes<?> bytes2) {
        this.bytes1 = bytes1;
        this.bytes2 = bytes2;
    }

    @Override
    public void close()
            throws IOException {
        if (bytes1 != null) bytes1.releaseLast();
        if (bytes2 != null) bytes2.releaseLast();
    }

    @Override
    public String toString() {
        return "MyBytes{" +
                "bytes1=" + bytes1 +
                ", bytes2=" + bytes2 +
                '}';
    }
}
