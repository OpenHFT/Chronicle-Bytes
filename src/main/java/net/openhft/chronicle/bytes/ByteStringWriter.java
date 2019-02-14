/*
 * Copyright 2016 higherfrequencytrading.com
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

package net.openhft.chronicle.bytes;

import net.openhft.chronicle.core.annotation.NotNull;

import java.io.IOException;
import java.io.Writer;
import java.nio.BufferOverflowException;

/**
 * A Writer for an underlying Bytes.  This moves the writePosition() up to the writeLimit();
 */
@SuppressWarnings("rawtypes")
class ByteStringWriter extends Writer {
    private final ByteStringAppender out;

    ByteStringWriter(ByteStringAppender out) {
        this.out = out;
    }

    @Override
    public void write(int c) throws IOException {
        try {
            out.append((char) c);

        } catch (@NotNull BufferOverflowException e) {
            throw new IOException(e);
        }
    }

    @Override
    public void write(String str) throws IOException {
        out.append(str);
    }

    @Override
    public void write(String str, int off, int len) throws IOException {
        out.append(str, off, off + len);
    }

    @NotNull
    @Override
    public Writer append(@NotNull CharSequence csq) throws IOException {
        out.append(csq);
        return this;
    }

    @NotNull
    @Override
    public Writer append(@NotNull CharSequence csq, int start, int end) throws IOException {
        out.append(csq, start, end);
        return this;
    }

    @NotNull
    @Override
    public Writer append(char c) throws IOException {
        out.append(c);
        return this;
    }

    @Override
    public void flush() throws IOException {

    }

    @Override
    public void close() throws IOException {

    }

    @Override
    public void write(char[] cbuf, int off, int len) throws IOException {
        for (int i = 0; i < len; i++)
            out.append(cbuf[i + off]);
    }
}
