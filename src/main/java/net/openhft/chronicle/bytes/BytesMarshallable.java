/*
 * Copyright 2016-2020 chronicle.software
 *
 * https://chronicle.software
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.openhft.chronicle.bytes;

import net.openhft.chronicle.core.annotation.DontChain;
import net.openhft.chronicle.core.io.IORuntimeException;

/**
 * An object which can be read or written directly to Bytes in a streaming manner.
 */
@DontChain
public interface BytesMarshallable extends ReadBytesMarshallable, WriteBytesMarshallable {
    @Override
    @SuppressWarnings("rawtypes")
    default void readMarshallable(BytesIn bytes) throws IORuntimeException {
        BytesUtil.readMarshallable(this, bytes);
    }

    @SuppressWarnings("rawtypes")
    @Override
    default void writeMarshallable(BytesOut bytes) {
        BytesUtil.writeMarshallable(this, bytes);
    }

    default String $toString() {
        HexDumpBytes bytes = new HexDumpBytes();
        writeMarshallable(bytes);
        String s = "# " + getClass().getName() + "\n" + bytes.toHexString();
        bytes.releaseLast();
        return s;
    }
}
