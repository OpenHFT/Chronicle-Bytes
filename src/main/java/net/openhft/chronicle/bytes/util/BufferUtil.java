/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes.util;

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
public final class BufferUtil {

    /**
     * Utility holder; not instantiable.
     */
    private BufferUtil() {
    }

    /**
     * Sets a {@link ByteBuffer}'s position using the Java 8 compatible {@link Buffer} API.
     *
     * @param byteBuffer buffer to update
     * @param newPosition new position
     */
    public static void position(ByteBuffer byteBuffer, int newPosition) {
        byteBuffer.position(newPosition);
    }

    /**
     * Invokes {@link Buffer#clear()} on the provided buffer.
     *
     * @param byteBuffer buffer to clear
     */
    public static void clear(ByteBuffer byteBuffer) {
        byteBuffer.clear();
    }

    /**
     * Invokes {@link Buffer#flip()} on the provided buffer.
     *
     * @param byteBuffer buffer to flip
     */
    public static void flip(ByteBuffer byteBuffer) {
        byteBuffer.flip();
    }

    /**
     * Sets a {@link ByteBuffer}'s limit using the Java 8 compatible {@link Buffer} API.
     *
     * @param byteBuffer buffer to update
     * @param newLimit new limit
     */
    public static void limit(ByteBuffer byteBuffer, int newLimit) {
        byteBuffer.limit(newLimit);
    }
}
