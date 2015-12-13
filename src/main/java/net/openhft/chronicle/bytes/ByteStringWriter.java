/*
 *     Copyright (C) 2015  higherfrequencytrading.com
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU Lesser General Public License as published by
 *     the Free Software Foundation, either version 3 of the License.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU Lesser General Public License for more details.
 *
 *     You should have received a copy of the GNU Lesser General Public License
 *     along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package net.openhft.chronicle.bytes;

import net.openhft.chronicle.core.io.IORuntimeException;

import java.io.IOException;
import java.io.Writer;
import java.nio.BufferOverflowException;

/**
 * A Writer for an underlying Bytes.  This moves the writePosition() up to the writeLimit();
 */
public class ByteStringWriter extends Writer {
    private final ByteStringAppender out;

    public ByteStringWriter(ByteStringAppender out) {
        this.out = out;
    }

    @Override
    public void write(int c) throws IOException {
        try {
            out.append(c);
        } catch (BufferOverflowException | IllegalArgumentException | IORuntimeException e) {
            throw new IOException(e);
        }
    }

    @Override
    public void write(String str) throws IOException {
        out.append(str);
    }

    @Override
    public void write(String str, int off, int len) throws IndexOutOfBoundsException, IOException {
        out.append(str, off, off + len);
    }

    @Override
    public Writer append(CharSequence csq) throws IOException {
        out.append(csq);
        return this;
    }

    @Override
    public Writer append(CharSequence csq, int start, int end) throws IndexOutOfBoundsException, IOException {
        out.append(csq, start, end);
        return this;
    }

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
