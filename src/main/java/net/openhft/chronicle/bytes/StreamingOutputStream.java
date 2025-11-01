/*
 * Copyright 2016-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes;

import net.openhft.chronicle.bytes.internal.NoBytesStore;
import net.openhft.chronicle.core.annotation.NonNegative;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.BufferOverflowException;

/**
 * An {@link OutputStream} adapter that writes its bytes to a
 * {@link StreamingDataOutput}. This allows Chronicle Bytes streams to be used
 * where a standard {@code OutputStream} is required.
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
    /**
     * Writes bytes from the given array to the underlying {@link StreamingDataOutput}.
     * Any {@link BufferOverflowException}, {@link IllegalArgumentException} or
     * {@link IllegalStateException} from the target is wrapped in an
     * {@link IOException}.
     */
    public void write(byte[] b, @NonNegative int off, @NonNegative int len)
            throws IOException {
        try {
            sdo.write(b, off, len);

        } catch (BufferOverflowException | IllegalArgumentException | IllegalStateException e) {
            throw new IOException(e);
        }
    }

    @Override
    /**
     * Writes a single byte value. Exceptions thrown by the target
     * {@link StreamingDataOutput} are wrapped in an {@link IOException}.
     */
    public void write(int b)
            throws IOException {
        try {
            sdo.writeUnsignedByte(0xff & b);

        } catch (BufferOverflowException | ArithmeticException | IllegalStateException e) {
            throw new IOException(e);
        }
    }
}
