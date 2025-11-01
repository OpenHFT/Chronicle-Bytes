/*
 * Copyright 2016-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes;

import net.openhft.chronicle.core.io.ClosedIllegalStateException;
import net.openhft.chronicle.core.io.ThreadingIllegalStateException;
import org.jetbrains.annotations.NotNull;

/**
 * A common base interface for streaming data access. It manages fundamental
 * buffer properties such as positions and limits and supports random access
 * semantics. Implementations are mainly used by higher level streaming
 * interfaces like {@link StreamingDataInput} and {@link StreamingDataOutput}.
 *
 * @param <S> type of the implementing class
 */
public interface StreamingCommon<S extends StreamingCommon<S>> extends RandomCommon {

    /**
     * Resets the read and write positions to {@link #start()} and sets the
     * effective limits to {@link #capacity()}. The underlying data is left
     * unchanged but the buffer behaves as if newly allocated.
     *
     * @return this instance for chaining
     * @throws ClosedIllegalStateException    if the resource has been released or closed
     * @throws ThreadingIllegalStateException if accessed by multiple threads in an unsafe way
     */
    @NotNull
    S clear() throws ClosedIllegalStateException, ThreadingIllegalStateException;
}
