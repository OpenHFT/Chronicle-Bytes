/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes.internal;

import net.openhft.chronicle.bytes.ByteStringParser;
import net.openhft.chronicle.core.annotation.NonNegative;
import net.openhft.chronicle.core.io.ClosedIllegalStateException;
import net.openhft.chronicle.core.io.ThreadingIllegalStateException;

import java.io.IOException;
import java.io.Reader;
import java.nio.BufferUnderflowException;

import static net.openhft.chronicle.bytes.internal.ReferenceCountedUtil.throwExceptionIfReleased;

/**
 * Reader adapter over {@link ByteStringParser} so APIs expecting a
 * {@link Reader} can consume bytes while sharing the parser position.
 */
@SuppressWarnings("rawtypes")
public class ByteStringReader extends Reader {
    private final ByteStringParser in;

    /**
     * Constructs with the supplied parser so this reader shares its position.
     *
     * @param in the parser to read from
     */
    public ByteStringReader(ByteStringParser in) {
        throwExceptionIfReleased(in);
        this.in = in;
    }

    /**
     * Reads one byte or -1 at end of stream.
     *
     * @return the next byte or -1
     */
    @Override
    public int read() {
        try {
            return in.readRemaining() > 0 ? in.readUnsignedByte() : -1;
        } catch (IllegalStateException e) {
            return -1;
        }
    }

    /**
     * Skips up to {@code n} bytes in the underlying parser.
     *
     * @param n number of bytes to skip
     * @return the number of bytes skipped
     * @throws IOException if an I/O error occurs
     */
    @Override
    public long skip(long n)
            throws IOException {
        long len = Math.min(in.readRemaining(), n);
        try {
            in.readSkip(len);

        } catch (BufferUnderflowException | IllegalStateException e) {
            throw new IOException("Failed to skip bytes from ByteStringParser", e);
        }
        return len;
    }

    /**
     * Reads characters into a buffer slice.
     *
     * @param cbuf destination buffer
     * @param off  start offset
     * @param len  max chars to read
     * @return chars read, or -1 at end of stream
     * @throws IOException if an I/O error occurs
     */
    @Override
    public int read(char[] cbuf, @NonNegative int off, @NonNegative int len)
            throws IOException {
        try {
            return in.read(cbuf, off, len);

        } catch (IllegalStateException e) {
            throw new IOException("Failed to read chars from ByteStringParser", e);
        }
    }

    /**
     * No-op close because the underlying parser is managed elsewhere.
     */
    @Override
    public void close() {
        // Do nothing
    }
}
