/*
 * Copyright 2014 Higher Frequency Trading http://www.higherfrequencytrading.com
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

import java.nio.ByteOrder;

abstract class CharSequenceAccessor
        implements Accessor.Read<CharSequence, CharSequence> {

    static final Accessor.Read<? super String, ?> stringAccessor;
    static  {
        initAccessor:
        {
            if (System.getProperty("java.vm.name").contains("HotSpot")) {
                if (System.getProperty("java.version").compareTo("1.7.0_06") >= 0) {
                    stringAccessor = HotSpotStringAccessor.INSTANCE;
                    break initAccessor;
                }
            }
            stringAccessor = nativeCharSequenceAccessor();
        }
    }

    static CharSequenceAccessor nativeCharSequenceAccessor() {
        return ByteOrder.nativeOrder() == ByteOrder.LITTLE_ENDIAN ? LITTLE_ENDIAN : BIG_ENDIAN;
    }

    static final CharSequenceAccessor LITTLE_ENDIAN = new CharSequenceAccessor() {

        @Override
        public ReadAccess<CharSequence> access(CharSequence source) {
            return CharSequenceAccess.LittleEndianCharSequenceAccess.INSTANCE;
        }
    };

    static final CharSequenceAccessor BIG_ENDIAN = new CharSequenceAccessor() {

        @Override
        public ReadAccess<CharSequence> access(CharSequence source) {
            return CharSequenceAccess.BigEndianCharSequenceAccess.INSTANCE;
        }
    };

    private CharSequenceAccessor() {}

    @Override
    public CharSequence handle(CharSequence source) {
        return source;
    }

    @Override
    public long offset(CharSequence source, long index) {
        return index * 2L;
    }

    @Override
    public long size(long size) {
        return size * 2L;
    }
}
