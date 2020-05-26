/*
 * Copyright 2016-2020 Chronicle Software
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

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.BufferOverflowException;

@SuppressWarnings("rawtypes")
public class StreamingOutputStream extends OutputStream {
    private StreamingDataOutput sdo;

    public StreamingOutputStream() {
        this(NoBytesStore.NO_BYTES);
    }

    public StreamingOutputStream(StreamingDataOutput sdo) {
        this.sdo = sdo;
    }

    @NotNull
    public StreamingOutputStream init(StreamingDataOutput sdo) {
        this.sdo = sdo;
        return this;
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        try {
            sdo.write(b, off, len);

        } catch (@NotNull BufferOverflowException | IllegalArgumentException e) {
            throw new IOException(e);
        }
    }

    @Override
    public void write(int b) throws IOException {
        try {
            sdo.writeUnsignedByte(0xff & b);

        } catch (@NotNull BufferOverflowException | IllegalArgumentException e) {
            throw new IOException(e);
        }
    }
}
