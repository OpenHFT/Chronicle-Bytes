/*
 * Copyright (c) 2016-2022 chronicle.software
 *
 *     https://chronicle.software
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

import net.openhft.chronicle.core.annotation.NonNegative;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.BufferOverflowException;

/**
 * A special kind of OutputStream implementation which writes data to a StreamingDataOutput instance.
 *
 * <p>This class provides a way to connect APIs expecting an OutputStream with data destinations
 * encapsulated in StreamingDataOutput instances.</p>
 *
 * @see StreamingDataOutput
 * @see OutputStream
 */
@SuppressWarnings("rawtypes")
public class StreamingOutputStream extends OutputStream {
    private StreamingDataOutput sdo;

    /**
     * Constructs a new StreamingOutputStream instance and initializes the data destination as an empty ByteStore.
     */
    public StreamingOutputStream() {
        this(NoBytesStore.NO_BYTES);
    }

    /**
     * Constructs a new StreamingOutputStream instance with a specific StreamingDataOutput as the data destination.
     *
     * @param sdo the StreamingDataOutput instance to write data to.
     */
    public StreamingOutputStream(StreamingDataOutput sdo) {
        this.sdo = sdo;
    }

    /**
     * Initializes this StreamingOutputStream instance with a specific StreamingDataOutput as the data destination.
     *
     * @param sdo the StreamingDataOutput instance to write data to.
     * @return this StreamingOutputStream instance, for chaining.
     */
    @NotNull
    public StreamingOutputStream init(StreamingDataOutput sdo) {
        this.sdo = sdo;
        return this;
    }

    @Override
    public void write(byte[] b, @NonNegative int off, @NonNegative int len)
            throws IOException {
        try {
            sdo.write(b, off, len);

        } catch (BufferOverflowException | IllegalArgumentException | IllegalStateException e) {
            throw new IOException(e);
        }
    }

    @Override
    public void write(int b)
            throws IOException {
        try {
            sdo.writeUnsignedByte(0xff & b);

        } catch (BufferOverflowException | ArithmeticException | IllegalStateException e) {
            throw new IOException(e);
        }
    }
}
