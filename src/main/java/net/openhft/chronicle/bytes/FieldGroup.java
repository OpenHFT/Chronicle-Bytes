/*
 * Copyright 2016-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
/**
 * Groups related fields so a contiguous {@link Bytes} view can be created via utilities such as
 * {@link Bytes#forFieldGroup(Object, String)}.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface FieldGroup {

    /**
     * Defines the name of the field group. Multiple fields with the same {@code value} are considered
     * part of the same logical group.
     *
     * @return the name of the group
     */
    String value();
}
