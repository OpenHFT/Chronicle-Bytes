/*
 * Copyright 2016 higherfrequencytrading.com
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

import java.io.IOException;
import java.io.OutputStream;
import java.nio.BufferOverflowException;

/**
 * Created by peter on 17/08/15.
 */
class StreamingOutputStream extends OutputStream {
    private final StreamingDataOutput sdo;

    public StreamingOutputStream(StreamingDataOutput sdo) {
        this.sdo = sdo;
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        try {
            sdo.write(b, off, len);

        } catch (BufferOverflowException | IllegalArgumentException e) {
            throw new IOException(e);
        }
    }

    @Override
    public void write(int b) throws IOException {
        try {
            sdo.writeUnsignedByte(b);

        } catch (BufferOverflowException | IllegalArgumentException e) {
            throw new IOException(e);
        }
    }
}
