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

import org.jetbrains.annotations.NotNull;

import java.io.Writer;

public interface ByteStringAppender<B extends ByteStringAppender<B>> extends StreamingDataOutput<B>, Appendable {

    default Writer writer() {
        return new ByteStringWriter(this);
    }


    @NotNull
    default B append(char ch) {
        BytesUtil.appendUTF(this, ch);
        return (B) this;
    }

    @NotNull
    default B append(@NotNull CharSequence cs) {
        return append(cs, 0, cs.length());
    }

    @NotNull
    default B append(long value) {
        BytesUtil.append(this, value);
        return (B) this;
    }

    @NotNull
    default B append(float f) {
        BytesUtil.append((StreamingDataOutput) this, f);
        return (B) this;
    }

    @NotNull
    default B append(double d) {
        BytesUtil.append((StreamingDataOutput) this, d);
        return (B) this;
    }

    @NotNull
    default B append(@NotNull CharSequence cs, int start, int end) {
        BytesUtil.appendUTF(this, cs, start, end - start);
        return (B) this;
    }

    @NotNull
    default B append8bit(@NotNull CharSequence cs) {
        if (cs instanceof BytesStore) {
            return write((BytesStore) cs);
        }
        int length = cs.length();
        for (int i = 0; i < length; i++) {
            char c = cs.charAt(i);
            if (c > 255) c = '?';
            writeUnsignedByte(c);
        }
        return (B) this;
    }

    default B append8bit(@NotNull CharSequence cs, int offset, int length) {
        if (cs instanceof BytesStore) {
            return write((BytesStore) cs, (long) offset, length);
        }
        for (int i = 0; i < length; i++) {
            char c = cs.charAt(offset + i);
            if (c > 255) c = '?';
            writeUnsignedByte(c);
        }
        return (B) this;
    }

    @NotNull
    default B append(long value, int digits) {
        BytesUtil.append((RandomDataOutput) this, writePosition(), value, digits);
        this.writeSkip(digits);
        return (B) this;
    }

    @NotNull
    default B appendDateMillis(long timeInMillis) {
        BytesUtil.appendDateMillis(this, timeInMillis);
        return (B) this;
    }

    @NotNull
    default B appendTimeMillis(long timeInMillis) {
        BytesUtil.appendTimeMillis(this, timeInMillis);
        return (B) this;
    }
}
