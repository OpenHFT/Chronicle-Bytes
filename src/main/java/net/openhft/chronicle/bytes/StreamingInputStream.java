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
import java.io.InputStream;

/**
 * Created by peter on 17/08/15.
 */
public class StreamingInputStream extends InputStream {
    final StreamingDataInput in;

    public StreamingInputStream(StreamingDataInput in) {
        this.in = in;
    }

    @Override
    public long skip(long n) throws IOException {
        long len = Math.min(in.readRemaining(), n);
        in.readSkip(len);
        return len;
    }

    @Override
    public int available() throws IOException {
        return (int) Math.min(Integer.MAX_VALUE, in.readRemaining());
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        try {
            return in.read(b, off, len);
        } catch (IORuntimeException e) {
            throw new IOException(e);
        }
    }

    @Override
    public int read() throws IOException {
        try {
            return in.readRemaining() > 0 ? in.readUnsignedByte() : -1;
        } catch (IORuntimeException e) {
            throw new IOException(e);
        }
    }
}
