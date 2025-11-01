/*
 * Copyright 2016-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes;

import net.openhft.chronicle.core.annotation.DontChain;

/**
 * Marker for objects that can be serialised to bytes.  Implementations may choose to embed
 * type or structural metadata so that the resulting message is self describing.
 */
@DontChain
public interface CommonMarshallable {

    /**
     * Indicates whether the serialised form should contain enough metadata for a generic parser to
     * understand it without prior knowledge of the concrete type.
     *
     * @return {@code true} by default
     */
    default boolean usesSelfDescribingMessage() {
        return true;
    }
}
