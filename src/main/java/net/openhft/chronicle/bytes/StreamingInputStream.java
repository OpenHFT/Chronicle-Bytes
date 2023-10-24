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

import net.openhft.chronicle.bytes.internal.NoBytesStore;
import net.openhft.chronicle.core.annotation.NonNegative;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.InputStream;
import java.nio.BufferUnderflowException;

/**
 * A special kind of InputStream implementation which reads data from a StreamingDataInput instance.
 *
 * <p>This class provides a way to connect APIs expecting an InputStream with data sources
 * encapsulated in StreamingDataInput instances.
 *
 * @see StreamingDataInput
 * @see InputStream
 */
@SuppressWarnings("rawtypes")
public class StreamingInputStream extends InputStream {

    private StreamingDataInput in;

    /**
     * Constructs a new StreamingInputStream instance and initializes the data source as an empty ByteStore.
     */
    public StreamingInputStream() {
        this(NoBytesStore.NO_BYTES);
    }

    /**
     * Constructs a new StreamingInputStream instance with a specific StreamingDataInput as the data source.
     *
     * @param in the StreamingDataInput instance to read data from.
     */
    public StreamingInputStream(StreamingDataInput in) {
        this.in = in;
    }

    /**
     * Initializes this StreamingInputStream instance with a specific StreamingDataInput as the data source.
     *
     * @param in the StreamingDataInput instance to read data from.
     * @return this StreamingInputStream instance, for chaining.
     */
    @NotNull
    public StreamingInputStream init(StreamingDataInput in) {
        this.in = in;
        return this;
    }

    @Override
    public long skip(long n)
            throws IOException {
        try {
            long len = Math.min(in.readRemaining(), n);
            in.readSkip(len);
            return len;
        } catch (BufferUnderflowException | IllegalStateException e) {
            throw new IOException(e);
        }
    }

    @Override
    public int available()
            throws IOException {
        return (int) Math.min(Integer.MAX_VALUE, in.readRemaining());
    }

    @Override
    public int read(byte[] b, @NonNegative int off, @NonNegative int len)
            throws IOException {
        try {
            if (len == 0) {
                return 0;
            }
            int len2 = in.read(b, off, len);
            return len2 == 0 ? -1 : len2;
        } catch (BufferUnderflowException | IllegalStateException e) {
            throw new IOException(e);
        }
    }

    @Override
    public int read()
            throws IOException {
        try {
            return in.readRemaining() > 0 ? in.readUnsignedByte() : -1;
        } catch (IllegalStateException e) {
            throw new IOException(e);
        }
    }
}
