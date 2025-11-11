/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes;

import net.openhft.chronicle.core.annotation.DontChain;
import net.openhft.chronicle.core.io.*;

import java.nio.BufferOverflowException;
import java.nio.BufferUnderflowException;

/**
 * Serialisable object that reads and writes its state directly to a
 * {@link Bytes} stream. Default implementations delegate to
 * {@link BytesUtil} utilities. The interface is marked with
 * {@link DontChain} to discourage chaining of implementations.
 */
@DontChain
public interface BytesMarshallable extends ReadBytesMarshallable, WriteBytesMarshallable {

    /**
     * {@inheritDoc}
     */
    @Override
    default boolean usesSelfDescribingMessage() {
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    default void readMarshallable(BytesIn<?> bytes)
            throws IORuntimeException, BufferUnderflowException, IllegalStateException, InvalidMarshallableException {
        BytesUtil.readMarshallable(this, bytes);
        ValidatableUtil.validate(this);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    default void writeMarshallable(BytesOut<?> bytes)
            throws IllegalStateException, BufferOverflowException, BufferUnderflowException, ArithmeticException, InvalidMarshallableException {
        ValidatableUtil.validate(this);
        BytesUtil.writeMarshallable(this, bytes);
    }

    /**
     * Dumps the binary form of this object as a hex string for debugging.
     */
    default String $toString() {
        ValidatableUtil.startValidateDisabled();
        try {
            HexDumpBytes bytes = new HexDumpBytes();
            writeMarshallable(bytes);
            String s = "# " + getClass().getName() + "\n" + bytes.toHexString();
            bytes.releaseLast();
            return s;
        } catch (Throwable e) {
            return e.toString();
        } finally {
            ValidatableUtil.endValidateDisabled();
        }
    }
}
