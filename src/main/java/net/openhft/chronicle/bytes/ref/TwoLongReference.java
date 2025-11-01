/*
 * Copyright 2016-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes.ref;

import net.openhft.chronicle.bytes.Byteable;
import net.openhft.chronicle.core.values.TwoLongValue;

/**
 * Reference to two contiguous 64-bit values.
 *
 * <p>The interface itself does not prescribe thread-safety; the
 * implementation decides.</p>
 *
 * @see BinaryTwoLongReference
 */
@SuppressWarnings("rawtypes")
public interface TwoLongReference extends TwoLongValue, Byteable {
    // This interface combines TwoLongValue and Byteable
    // Specific method declarations are not necessary here as they are inherited
}
