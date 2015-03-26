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

import java.lang.reflect.Field;

final class HotSpotStringAccessor implements Accessor.Read<String, char[]> {
    public static final HotSpotStringAccessor INSTANCE = new HotSpotStringAccessor();

    private static final long valueOffset;

    static {
        try {
            Field valueField = String.class.getDeclaredField("value");
            valueOffset = NativeAccess.U.objectFieldOffset(valueField);
        } catch (NoSuchFieldException e) {
            throw new AssertionError(e);
        }
    }

    private HotSpotStringAccessor() {}

    @Override
    public ReadAccess<char[]> access(String source) {
        return NativeAccess.instance();
    }

    @Override
    public char[] handle(String source) {
        return (char[]) NativeAccess.U.getObject(source, valueOffset);
    }

    @Override
    public long offset(String source, long index) {
        return ArrayAccessors.Char.INSTANCE.offset(null, index);
    }

    @Override
    public long size(long size) {
        return size * 2L;
    }
}
