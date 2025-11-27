/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes.internal;

import java.nio.Buffer;
import java.nio.ByteBuffer;

/**
 * Internal helper methods for working with {@link ByteBuffer} in a way that is
 * compatible with Java 8 and later.
 *
 * <p>Do not call {@code ByteBuffer.position(int)} or similar methods directly in
 * library code where the compiled bytecode must run on Java 8. Use these helpers
 * instead so the required cast to {@link Buffer} is explicit and not removed as
 * a "redundant" cast.</p>
 */
// TODO Check this has been used everywhere needed.
public final class BufferUtil {

    private BufferUtil() {
    }

    public static void setPosition(ByteBuffer byteBuffer, int newPosition) {
        ((Buffer) byteBuffer).position(newPosition);
    }

    public static void clear(ByteBuffer byteBuffer) {
        ((Buffer) byteBuffer).clear();
    }

    public static void flip(ByteBuffer byteBuffer) {
        ((Buffer) byteBuffer).flip();
    }

    public static void setLimit(ByteBuffer byteBuffer, int newLimit) {
        ((Buffer) byteBuffer).limit(newLimit);
    }
}

