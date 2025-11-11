/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes.internal;

import net.openhft.chronicle.core.Jvm;

import java.lang.reflect.Field;
import java.nio.ByteBuffer;

/**
 * Internal utility for low level manipulation of {@link ByteBuffer} objects,
 * primarily direct buffers. It relies on reflection to modify the private
 * address and capacity fields, so must be used with great care.
 */
public final class ByteBuffers {
    private ByteBuffers() {
    }

    /** reflected field for ByteBuffer.address */
    private static final Field ADDRESS;
    /** reflected field for ByteBuffer.capacity */
    private static final Field CAPACITY;

    static {
        ByteBuffer direct = ByteBuffer.allocateDirect(0);
        Field address = null;
        Field capacity = null;
        try {
            address = Jvm.getField(direct.getClass(), "address");
            capacity = Jvm.getField(direct.getClass(), "capacity");
        } catch (Throwable t) {
            Jvm.warn().on(ByteBuffers.class, "Unable to access direct ByteBuffer fields", t);
        }
        ADDRESS = address;
        CAPACITY = capacity;
    }

    /**
     * Directly sets the internal {@code address} and {@code capacity} fields of
     * the given direct {@link ByteBuffer}.
     * <p>
     * <strong>Warning:</strong> this bypasses all normal safety mechanisms. The
     * caller must ensure the provided address and capacity describe a valid
     * memory region or the JVM may crash.
     *
     * @param buffer   the direct buffer to modify
     * @param address  native address for the buffer
     * @param capacity new capacity value (truncated to {@code int})
     * @throws AssertionError if reflective access fails
     */
    public static void setAddressCapacity(ByteBuffer buffer, long address, long capacity) {
        if (ADDRESS == null || CAPACITY == null)
            throw new UnsupportedOperationException("Direct ByteBuffer fields not accessible");
        int cap = Math.toIntExact(capacity);
        try {
            ADDRESS.setLong(buffer, address);
            CAPACITY.setInt(buffer, cap);
        } catch (IllegalAccessException | IllegalArgumentException e) {
            throw new AssertionError(e);
        }
    }
}
