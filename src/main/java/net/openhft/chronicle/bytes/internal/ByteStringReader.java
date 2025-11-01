/*
 * Copyright 2016-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
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
 * A Reader wrapper for Bytes. This Reader moves the readPosition() of the underlying Bytes up to the readLimit().
 */
@SuppressWarnings("rawtypes")
public class ByteStringReader extends Reader {
    private final ByteStringParser in;

    /**
     * Constructs a new ByteStringReader with the provided ByteStringParser.
     *
     * @param in The ByteStringParser to be used.
     * @throws ClosedIllegalStateException    If the resource has been released or closed.
     * @throws ThreadingIllegalStateException If this resource was accessed by multiple threads in an unsafe way
     */
    public ByteStringReader(ByteStringParser in) {
        throwExceptionIfReleased(in);
        this.in = in;
    }

    /**
     * Reads a single byte from the underlying ByteStringParser.
     *
     * @return The next byte of data, or -1 if the end of the stream is reached.
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
     * Skips over and discards n bytes of data from this input stream.
     *
     * @param n The number of bytes to be skipped.
     * @return The actual number of bytes skipped.
     * @throws IOException if an I/O error occurs.
     */
    @Override
    public long skip(long n)
            throws IOException {
        long len = Math.min(in.readRemaining(), n);
        try {
            in.readSkip(len);

        } catch (BufferUnderflowException | IllegalStateException e) {
            throw new IOException(e);
        }
        return len;
    }

    /**
     * Reads characters into a portion of an array.
     *
     * @param cbuf Destination buffer.
     * @param off  Offset at which to start storing characters.
     * @param len  Maximum number of characters to read.
     * @return The number of characters read, or -1 if the end of the stream has been reached.
     * @throws IOException If an I/O error occurs.
     */
    @Override
    public int read(char[] cbuf, @NonNegative int off, @NonNegative int len)
            throws IOException {
        try {
            return in.read(cbuf, off, len);

        } catch (IllegalStateException e) {
            throw new IOException(e);
        }
    }

    /**
     * Closes the reader and releases any system resources associated with it.
     */
    @Override
    public void close() {
        // Do nothing
    }
}
