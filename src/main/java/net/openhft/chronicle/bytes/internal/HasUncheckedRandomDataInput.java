/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes.internal;

import org.jetbrains.annotations.NotNull;

/**
 * Provides a means to obtain an {@link UncheckedRandomDataInput} view for fast
 * reads where bounds checks have already been performed by the caller.
 */
@FunctionalInterface
public interface HasUncheckedRandomDataInput {

    /**
     * @return a view for random reads with minimal bounds checking
     */
    @NotNull
    UncheckedRandomDataInput acquireUncheckedInput();

}
