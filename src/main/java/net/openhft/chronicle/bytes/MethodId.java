/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes;

import java.lang.annotation.*;

/**
 * Annotation that denotes a method as having a numeric identifier for efficient encoding.
 *
 * <p>
 * Applying this annotation to a method allows it to be associated with a numeric value,
 * which can be leveraged during the encoding process to enhance efficiency. Numeric values
 * are more efficient to encode and decode than string representations, especially in
 * high-performance or resource-constrained environments.
 * <p>
 * The numeric identifier is user-defined and should be unique to ensure correct mapping.
 * For simpler decoding, a character can be used as the numeric identifier, leveraging
 * its underlying ASCII or Unicode numeric value.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@Inherited
public @interface MethodId {
    /**
     * The unique numeric identifier associated with the method.
     *
     * @return The unique numeric identifier associated with the method.
     */
    long value();
}
