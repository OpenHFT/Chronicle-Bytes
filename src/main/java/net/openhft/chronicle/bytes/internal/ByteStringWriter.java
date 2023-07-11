/*
 * Copyright (c) 2016-2022 chronicle.software
 *
 *     https://chronicle.software
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.openhft.chronicle.bytes.internal;

import net.openhft.chronicle.bytes.ByteStringAppender;
import net.openhft.chronicle.core.Jvm;
import net.openhft.chronicle.core.annotation.NonNegative;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.Writer;

import static net.openhft.chronicle.bytes.internal.ReferenceCountedUtil.throwExceptionIfReleased;

/**
 * A Writer for an underlying Bytes. This moves the writePosition() up to the writeLimit().
 */
@SuppressWarnings("rawtypes")
public class ByteStringWriter extends Writer {
    private final ByteStringAppender out;

    /**
     * Constructs a new ByteStringWriter with the provided ByteStringAppender.
     *
     * @param out The ByteStringAppender to be used.
     * @throws IllegalStateException if the input ByteStringAppender is released.
     */
    public ByteStringWriter(ByteStringAppender out) {
        throwExceptionIfReleased(out);
        this.out = out;
    }

    /**
     * Writes a single character.
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
            throw new IOException(e);
        }
    }

    /**
     * Writes a string.
     *
     * @param str String to be written.
     * @throws IOException if an I/O error occurs.
     */
    @Override
    public void write(@NotNull String str)
            throws IOException {
        out.append(str);
    }

    /**
     * Writes a portion of a string.
     *
     * @param str String to be written.
     * @param off Offset from which to start reading characters.
     * @param len Number of characters to be written.
     * @throws IOException if an I/O error occurs.
     */
    @Override
    public void write(@NotNull String str, @NonNegative int off, @NonNegative int len)
            throws IOException {
        out.append(str, off, off + len);
    }

    /**
     * Appends a character sequence.
     *
     * @param csq The character sequence to append.
     * @return This writer
     * @throws IOException if an I/O error occurs.
     */
    @NotNull
    @Override
    public Writer append(@NotNull CharSequence csq)
            throws IOException {
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
     */
    @NotNull
    @Override
    public Writer append(@NotNull CharSequence csq, @NonNegative int start, @NonNegative int end) {
        out.append(csq, start, end);
        return this;
    }

    /**
     * Appends a single character.
     *
     * @param c The character to append.
     * @return This writer
     */
    @NotNull
    @Override
    public Writer append(char c) {
        try {
            out.append(c);
        } catch (IllegalStateException e) {
            throw Jvm.rethrow(e);
        }
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
     * @throws IOException if an I/O error occurs.
     */
    @Override
    public void write(char[] cbuf, @NonNegative int off, @NonNegative int len)
            throws IOException {
        try {
            for (int i = 0; i < len; i++)
                out.append(cbuf[i + off]);
        } catch (IllegalStateException e) {
            throw Jvm.rethrow(e);
        }
    }
}
