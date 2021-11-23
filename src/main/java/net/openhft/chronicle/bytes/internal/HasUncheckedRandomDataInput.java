package net.openhft.chronicle.bytes.internal;

import org.jetbrains.annotations.NotNull;

@FunctionalInterface
public interface HasUncheckedRandomDataInput {

    /**
     * Returns a view of a memory segment providing memory read operations with potentially unchecked
     * memory boundaries.
     *
     * @return an unchecked view
     */
    @NotNull
    UncheckedRandomDataInput acquireUncheckedInput();

}