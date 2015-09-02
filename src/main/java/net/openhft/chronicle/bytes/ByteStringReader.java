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

import java.io.IOException;
import java.io.Reader;
import java.nio.BufferUnderflowException;

/**
 * A Reader wrapper for Bytes.  This Reader moves the readPosition() of the underlying Bytes up to the readLimit()
 */
public class ByteStringReader extends Reader {
    private final ByteStringParser in;

    public ByteStringReader(ByteStringParser in) {
        this.in = in;
    }

    @Override
    public int read() throws IOException {
        try {
            return in.readRemaining() > 0 ? in.readUnsignedByte() : -1;
        } catch (IORuntimeException e) {
            throw new IOException(e);
        }
    }

    @Override
    public long skip(long n) throws IOException {
        long len = Math.min(in.readRemaining(), n);
        try {
            in.readSkip(len);
        } catch (BufferUnderflowException | IORuntimeException e) {
            throw new IOException(e);
        }
        return len;
    }

    @Override
    public int read(char[] cbuf, int off, int len) throws IOException {
        try {
            return in.read(cbuf, off, len);
        } catch (IORuntimeException e) {
            throw new IOException(e);
        }
    }

    @Override
    public void close() throws IOException {
    }
}
