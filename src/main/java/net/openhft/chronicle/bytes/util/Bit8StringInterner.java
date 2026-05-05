/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes.util;

import net.openhft.chronicle.bytes.BytesStore;
import net.openhft.chronicle.core.annotation.NonNegative;
import net.openhft.chronicle.core.io.ClosedIllegalStateException;
import net.openhft.chronicle.core.io.ThreadingIllegalStateException;
import net.openhft.chronicle.core.pool.StringBuilderPool;
import net.openhft.chronicle.core.scoped.ScopedResource;
import net.openhft.chronicle.core.scoped.ScopedResourcePool;
import org.jetbrains.annotations.NotNull;

import java.nio.BufferUnderflowException;

/**
 * Interns strings backed by 8-bit bytes (ISO-8859-1 style).
 */
public class Bit8StringInterner extends AbstractInterner<String> {

    /**
     * Pool of StringBuilder instances used during decoding.
     */
    private static final ScopedResourcePool<StringBuilder> SBP = StringBuilderPool.createThreadLocal(1);

    /**
     * Constructs with the given capacity to size the interning tables.
     *
     * @param capacity the initial capacity
     */
    public Bit8StringInterner(int capacity) {
        super(capacity);
    }

    /**
     * Decodes an 8-bit sequence without advancing the read position.
     *
     * @param cs     the source bytes
     * @param length number of bytes to read
     * @return the resulting string
     * @throws BufferUnderflowException if there is not enough remaining data
     */
    @SuppressWarnings("rawtypes")
    @Override
    @NotNull
    protected String getValue(@NotNull BytesStore<?, ?> cs, @NonNegative int length) throws IllegalStateException, BufferUnderflowException {
        try (ScopedResource<StringBuilder> sbTl = SBP.get()) {
            StringBuilder sb = sbTl.get();
            for (int i = 0; i < length; i++)
                sb.append((char) cs.readUnsignedByte(cs.readPosition() + i));
            return sb.toString();
        }
    }
}
