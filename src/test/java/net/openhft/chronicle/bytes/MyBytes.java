/*
 * Copyright (c) 2016-2022 chronicle.software
 *
 *       https://chronicle.software
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

import java.io.Closeable;
import java.io.IOException;

@SuppressWarnings("rawtypes")
class MyBytes implements BytesMarshallable, Closeable {
    Bytes<?> bytes1;
    Bytes<?> bytes2;

    public MyBytes() {
    }

    public MyBytes(Bytes<?> bytes1, Bytes<?> bytes2) {
        this.bytes1 = bytes1;
        this.bytes2 = bytes2;
    }

    @Override
    public void close()
            throws IOException {
        if (bytes1 != null) bytes1.releaseLast();
        if (bytes2 != null) bytes2.releaseLast();
    }

    @Override
    public String toString() {
        return "MyBytes{" +
                "bytes1=" + bytes1 +
                ", bytes2=" + bytes2 +
                '}';
    }
}
