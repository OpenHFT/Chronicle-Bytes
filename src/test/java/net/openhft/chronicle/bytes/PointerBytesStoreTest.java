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

import junit.framework.TestCase;

public class PointerBytesStoreTest extends TestCase {

    public void testWrap() throws IllegalArgumentException {
        NativeBytesStore<Void> nbs = NativeBytesStore.nativeStore(10000);

        PointerBytesStore pbs = BytesStore.nativePointer();
        pbs.set(nbs.address(nbs.start()), nbs.realCapacity());

        long nanoTime = System.nanoTime();
        pbs.writeLong(0L, nanoTime);

        assertEquals(nanoTime, nbs.readLong(0L));
    }
}