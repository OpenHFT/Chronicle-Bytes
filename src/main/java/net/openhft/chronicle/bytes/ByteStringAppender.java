/*
 * Copyright 2015 Higher Frequency Trading
 *
 * http://www.higherfrequencytrading.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.openhft.chronicle.bytes;

public interface ByteStringAppender<B extends ByteStringAppender<B, A, AT>,
        A extends WriteAccess<AT>, AT> extends StreamingDataOutput<B, A, AT> {
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
        BytesUtil.append(this, f);
        return (B) this;
    }

    default B append(double d) {
        BytesUtil.append(this, d);
        return (B) this;
    }

    default B append(CharSequence cs, int start, int end) {
        BytesUtil.appendUTF(this, cs, start, end - start);
        return (B) this;
    }

    default B append(long value, int digits) {
        BytesUtil.append((RandomDataOutput) this, position(), value, digits);
        this.skip(digits);
        return (B) this;
    }

}
