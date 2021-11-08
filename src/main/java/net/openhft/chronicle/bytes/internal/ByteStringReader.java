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

import net.openhft.chronicle.bytes.ByteStringParser;

import java.io.IOException;
import java.io.Reader;
import java.nio.BufferUnderflowException;

import static net.openhft.chronicle.bytes.internal.ReferenceCountedUtil.throwExceptionIfReleased;

/**
 * A Reader wrapper for Bytes.  This Reader moves the readPosition() of the underlying Bytes up to the readLimit()
 */
@SuppressWarnings("rawtypes")
public class ByteStringReader extends Reader {
    private final ByteStringParser in;

    public ByteStringReader(ByteStringParser in) {
        throwExceptionIfReleased(in);
        this.in = in;
    }

    @Override
    public int read() {
        try {
            return in.readRemaining() > 0 ? in.readUnsignedByte() : -1;
        } catch (IllegalStateException e) {
            return -1;
        }
    }

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

    @Override
    public int read(char[] cbuf, int off, int len)
            throws IOException {
        try {
            return in.read(cbuf, off, len);

        } catch (IllegalStateException e) {
            throw new IOException(e);
        }
    }

    @Override
    public void close() {
        // Do nothing
    }
}
