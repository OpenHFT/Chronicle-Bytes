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
import java.io.OutputStream;

/**
 * Created by peter on 17/08/15.
 */
class StreamingOutputStream extends OutputStream {
    private final StreamingDataOutput sdo;

    public StreamingOutputStream(StreamingDataOutput sdo) {
        this.sdo = sdo;
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        sdo.write(b, off, len);
    }

    @Override
    public void write(int b) throws IOException {
        sdo.writeUnsignedByte(b);
    }
}
