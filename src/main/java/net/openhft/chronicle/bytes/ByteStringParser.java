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

public interface ByteStringParser<B extends ByteStringParser<B, A, AT>,
        A extends ReadAccess<AT>, AT> extends StreamingDataInput<B, A, AT> {
    default String parseUTF(StopCharTester stopCharTester) {
        return BytesUtil.parseUTF(this, stopCharTester);
    }

    default void parseUTF(Appendable sb, StopCharTester stopCharTester) {
        BytesUtil.parseUTF(this, sb, stopCharTester);
    }

    default void parseUTF(Appendable sb, StopCharsTester stopCharsTester) {
        BytesUtil.parseUTF(this, sb, stopCharsTester);
    }

    default void parse8bit(Appendable sb, StopCharsTester stopCharsTester) {
        if (sb instanceof StringBuilder)
            BytesUtil.parse8bit(this, (StringBuilder) sb, stopCharsTester);
        else
            BytesUtil.parse8bit(this, (Bytes) sb, stopCharsTester);
    }

    default long parseLong() {
        return BytesUtil.parseLong(this);
    }

    default double parseDouble() {
        return BytesUtil.parseDouble(this);
    }

    default boolean skipTo(StopCharTester tester) {
        return BytesUtil.skipTo(this, tester);
    }
}
