/*
 * Copyright 2016-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes.perf;

import net.openhft.chronicle.bytes.MappedBytes;

import java.io.FileNotFoundException;

public class ChunkedMappedBytesReadWriteJLBH {

    public static void main(String[] args) throws FileNotFoundException {
        try (final MappedBytes bytes = MappedBytes.mappedBytes("/dev/shm/ChunkedMappedBytesArrayReadWriteJLBH", 1024 * 1024)) {
            BytesReadWriteJLBH.runForBytes(bytes);
        }
    }
}
