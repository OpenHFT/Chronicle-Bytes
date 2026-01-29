/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes.internal;

import net.openhft.chronicle.bytes.ByteStringAppender;
import net.openhft.chronicle.core.annotation.NonNegative;
import net.openhft.chronicle.core.io.ClosedIllegalStateException;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.Writer;

import static net.openhft.chronicle.bytes.internal.ReferenceCountedUtil.throwExceptionIfReleased;

/**
 * Writer adapter for an underlying Bytes that forwards characters and advances
 * the writePosition up to the writeLimit because some APIs require a
 * {@link Writer} rather than a {@link ByteStringAppender}.
 */
@SuppressWarnings({"rawtypes", "checkstyle:MMOverusedWord"})
public class ByteStringWriter extends Writer {
    private final ByteStringAppender out;

    /**
     * Constructs a new ByteStringWriter with the provided ByteStringAppender.
     *
     * @param out The ByteStringAppender to be used.
     * @throws ClosedIllegalStateException if the input ByteStringAppender is released.
     */
    public ByteStringWriter(ByteStringAppender out) {
        throwExceptionIfReleased(out);
        this.out = out;
    }

    /**
     * Writes a single character to the underlying Bytes via the appender.
     *
     * @param c int specifying a character to be written.
     * @throws IOException if an I/O error occurs.
     */
    @Override
    public void write(int c)
            throws IOException {
        try {
            out.append((char) c);

        } catch (IllegalStateException e) {
            throw new IOException("Failed to write single character to Bytes appender", e);
        }
    }

    /**
     * Writes the full string to the underlying Bytes appender.
     *
     * @param str String to be written.
     */
    @Override
    public void write(@NotNull String str) {
        out.append(str);
    }

    /**
     * Writes a substring region to the underlying Bytes appender.
     *
     * @param str String to be written.
     * @param off Offset from which to start reading characters.
     * @param len Number of characters to be written.
     */
    @Override
    public void write(@NotNull String str, @NonNegative int off, @NonNegative int len) {
        out.append(str, off, off + len);
    }

    /**
     * Appends a character sequence to the underlying Bytes appender.
     *
     * @param csq The character sequence to append.
     * @return This writer
     */
    @NotNull
    @Override
    public Writer append(@NotNull CharSequence csq) {
        out.append(csq);
        return this;
    }

    /**
     * Appends a portion of a character sequence.
     *
     * @param csq   The character sequence to append.
     * @param start The index of the first character to append.
     * @param end   The index of the character following the last character to append.
     * @return This writer
     * @throws ClosedIllegalStateException if the input ByteStringAppender is released.
     */
    @NotNull
    @Override
    public Writer append(@NotNull CharSequence csq, @NonNegative int start, @NonNegative int end) {
        out.append(csq, start, end);
        return this;
    }

    /**
     * Appends a single character to the underlying Bytes appender.
     *
     * @param c The character to append.
     * @return This writer
     * @throws ClosedIllegalStateException if the input ByteStringAppender is released.
     */
    @NotNull
    @Override
    public Writer append(char c) {
        out.append(c);
        return this;
    }

    /**
     * Flushes the stream. If the stream has saved any characters from the various write() methods in a buffer, write them immediately to their intended destination.
     */
    @Override
    public void flush() {
        // Do nothing
    }

    /**
     * Closes the writer, flushing it first. Once a writer has been closed, further write() invocations will cause an IOException to be thrown.
     */
    @Override
    public void close() {
        // Do nothing
    }

    /**
     * Writes a portion of an array of characters.
     *
     * @param cbuf Array of characters.
     * @param off  Offset from which to start reading characters.
     * @param len  Number of characters to be written.
     */
    @Override
    public void write(char[] cbuf, @NonNegative int off, @NonNegative int len) {
        for (int i = 0; i < len; i++)
            out.append(cbuf[i + off]);
    }
}
