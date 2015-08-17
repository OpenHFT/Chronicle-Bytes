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
import java.io.Writer;

/**
 * Created by peter on 17/08/15.
 */
public class ByteStringWriter extends Writer {
    private final ByteStringAppender out;

    public ByteStringWriter(ByteStringAppender out) {
        this.out = out;
    }

    @Override
    public void write(int c) {
        out.append(c);
    }

    @Override
    public void write(String str) {
        out.append(str);
    }

    @Override
    public void write(String str, int off, int len) {
        out.append(str, off, len);
    }

    @Override
    public Writer append(CharSequence csq) {
        out.append(csq);
        return this;
    }

    @Override
    public Writer append(CharSequence csq, int start, int end) {
        out.append(csq, start, end);
        return this;
    }

    @Override
    public Writer append(char c) {
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
    public void write(char[] cbuf, int off, int len) {
        for (int i = 0; i < len; i++) {
            out.append(cbuf[i + off]);
        }
    }
}
