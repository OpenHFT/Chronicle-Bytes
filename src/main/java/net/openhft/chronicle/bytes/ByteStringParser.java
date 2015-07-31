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

import net.openhft.chronicle.core.annotation.ForceInline;
import org.jetbrains.annotations.NotNull;

interface ByteStringParser<B extends ByteStringParser<B>> extends StreamingDataInput<B> {
    @NotNull
    @ForceInline
    default String parseUTF(@NotNull StopCharTester stopCharTester) {
        return BytesUtil.parseUTF(this, stopCharTester);
    }

    @ForceInline
    default void parseUTF(@NotNull Appendable sb, @NotNull StopCharTester stopCharTester) {
        BytesUtil.parseUTF(this, sb, stopCharTester);
    }

    @ForceInline
    default void parseUTF(@NotNull Appendable sb, @NotNull StopCharsTester stopCharsTester) {
        BytesUtil.parseUTF(this, sb, stopCharsTester);
    }

    @ForceInline
    default void parse8bit(Appendable sb, @NotNull StopCharsTester stopCharsTester) {
        if (sb instanceof StringBuilder)
            BytesUtil.parse8bit(this, (StringBuilder) sb, stopCharsTester);
        else
            BytesUtil.parse8bit(this, (Bytes) sb, stopCharsTester);
    }

    @ForceInline
    default long parseLong() {
        return BytesUtil.parseLong(this);
    }

    @ForceInline
    default double parseDouble() {
        return BytesUtil.parseDouble(this);
    }

    @ForceInline
    default boolean skipTo(@NotNull StopCharTester tester) {
        return BytesUtil.skipTo(this, tester);
    }
}
