/*
 * Copyright 2016-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes.ref;

import net.openhft.chronicle.bytes.Byteable;
import net.openhft.chronicle.core.values.LongValue;

/**
 * Represents a reference to a 64-bit long value, which is both {@link Byteable} and {@link LongValue}.
 *
 * <p>The {@code LongReference} interface provides a contract for classes that need to represent
 * a reference to a long value which can be read from or written to a {@link net.openhft.chronicle.bytes.BytesStore}.
 * This interface is particularly useful for memory-mapped values, where changes to the value are reflected in memory.
 *
 * <p>Classes implementing this interface are expected to provide efficient, low-level access
 * to the underlying bytes of the referenced long value.
 *
 * @see Byteable
 * @see LongValue
 */
@SuppressWarnings("rawtypes")
public interface LongReference extends LongValue, Byteable {
    // This interface combines LongValue and Byteable
    // Specific method declarations are not necessary here as they are inherited
}
