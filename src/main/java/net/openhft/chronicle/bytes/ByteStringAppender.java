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

public interface ByteStringAppender<B extends ByteStringAppender<B>> extends StreamingDataOutput<B>, Appendable {

    default B append(char ch) {
        BytesUtil.appendUTF(this, ch);
        return (B) this;
    }

    default B append(CharSequence cs) {
        return append(cs, 0, cs.length());
    }

    default B append(long value) {
        BytesUtil.append(this, value);
        return (B) this;
    }

    default B append(float f) {
        BytesUtil.append((StreamingDataOutput) this, f);
        return (B) this;
    }

    default B append(double d) {
        BytesUtil.append((StreamingDataOutput) this, d);
        return (B) this;
    }

    default B append(CharSequence cs, int start, int end) {
        BytesUtil.appendUTF(this, cs, start, end - start);
        return (B) this;
    }

    default B append8bit(CharSequence cs) {
        BytesUtil.append8bit(this, cs, 0, cs.length());
        return (B) this;
    }

    default B append(long value, int digits) {
        BytesUtil.append((RandomDataOutput) this, position(), value, digits);
        this.skip(digits);
        return (B) this;
    }
}
