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

import net.openhft.chronicle.bytes.IORuntimeException;
import net.openhft.chronicle.bytes.StopCharTester;
import net.openhft.chronicle.core.annotation.ForceInline;

public class EscapingStopCharTester implements StopCharTester {
    private final StopCharTester sct;
    private boolean escaped = false;

    public EscapingStopCharTester(StopCharTester sct) {
        this.sct = sct;
    }

    @Override
    @ForceInline
    public boolean isStopChar(int ch) throws IORuntimeException {
        if (escaped) {
            escaped = false;
            return false;
        }
        if (ch == '\\') {
            escaped = true;
            return false;
        }
        return sct.isStopChar(ch);
    }
}
