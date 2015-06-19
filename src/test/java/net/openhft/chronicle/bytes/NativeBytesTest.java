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

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Created by daniel on 17/04/15.
 */
public class NativeBytesTest {

    @Test
    public void testWriteBytesWhereResizeNeeded0() {
        Bytes bytes0 = NativeBytes.nativeBytes();
        Bytes<byte[]> wrap0 = Bytes.wrap("Hello".getBytes());
        bytes0.write(wrap0);
        bytes0.flip();
        assertEquals("Hello", bytes0.toString());
    }

    @Test
    public void testWriteBytesWhereResizeNeeded() {
        Bytes bytes1 = NativeBytes.nativeBytes(1);
        Bytes<byte[]> wrap1 = Bytes.wrap("Hello".getBytes());
        bytes1.write(wrap1);
        bytes1.flip();
        assertEquals("Hello", bytes1.toString());
    }
}