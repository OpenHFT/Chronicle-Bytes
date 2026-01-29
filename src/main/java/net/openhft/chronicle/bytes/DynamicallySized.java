/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes;

/**
 * Marker for {@link Byteable} objects whose encoded length may vary with their
 * state, such as strings or collections. Implementations of this interface
 * indicate that the serialised size cannot be determined statically because
 * it depends on runtime data.
 */
public interface DynamicallySized {
}
