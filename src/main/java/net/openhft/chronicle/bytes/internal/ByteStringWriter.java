/*
 * Copyright 2016-2020 chronicle.software
 *
 * https://chronicle.software
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
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.Writer;

/**
 * A Writer for an underlying Bytes.  This moves the writePosition() up to the writeLimit();
 */
@SuppressWarnings("rawtypes")
public class ByteStringWriter extends Writer {
    private final ByteStringAppender out;

    public ByteStringWriter(ByteStringAppender out) {
        this.out = out;
    }

    @Override
    public void write(int c)
            throws IOException {
        try {
            out.append((char) c);

        } catch (IllegalStateException e) {
            throw new IOException(e);
        }
    }

    @Override
    public void write(@NotNull String str)
            throws IOException {
        out.append(str);
    }

    @Override
    public void write(@NotNull String str, int off, int len)
            throws IOException {
        out.append(str, off, off + len);
    }

    @NotNull
    @Override
    public Writer append(@NotNull CharSequence csq)
            throws IOException {
        out.append(csq);
        return this;
    }

    @NotNull
    @Override
    public Writer append(@NotNull CharSequence csq, int start, int end) {
        out.append(csq, start, end);
        return this;
    }

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

    @Override
    public void flush() {

    }

    @Override
    public void close() {

    }

    @Override
    public void write(char[] cbuf, int off, int len)
            throws IOException {
        try {
            for (int i = 0; i < len; i++)
                out.append(cbuf[i + off]);
        } catch (IllegalStateException e) {
            throw Jvm.rethrow(e);
        }
    }
}
