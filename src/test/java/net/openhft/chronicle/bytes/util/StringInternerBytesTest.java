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

package net.openhft.chronicle.bytes.util;

import net.openhft.chronicle.bytes.Bytes;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Created by peter on 17/09/15.
 */
public class StringInternerBytesTest {

    @Test
    public void testIntern() throws Exception {
        StringInternerBytes si = new StringInternerBytes(128);
        for (int i = 0; i < 100; i++) {
            Bytes b = Bytes.from("key" + i);
            si.intern(b, (int) b.readRemaining());
        }
        assertEquals(82, si.valueCount());
    }
}