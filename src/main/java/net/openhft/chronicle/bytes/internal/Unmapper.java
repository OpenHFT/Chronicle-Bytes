/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes.internal;

import net.openhft.chronicle.core.Jvm;
import net.openhft.chronicle.core.OS;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

/**
 * Runnable that unmaps a memory region when executed, typically via a cleaner
 * service.
 */
public final class Unmapper implements Runnable {
    private final long size;

    private final int pageSize;

    private volatile long address;

    /**
     * @param address start of the region
     * @param size    size of the region
     * @param pageSize operating system page size
     */
    public Unmapper(long address, long size, int pageSize) throws IllegalStateException {

        assert (address != 0);
        this.address = address;
        this.size = size;
        this.pageSize = pageSize;
    }

    /**
     * Performs the unmap. If already unmapped the call is ignored.
     */
    @Override
    public void run() {
        if (address == 0)
            return;

        try {
            OS.unmap(address, size, pageSize);
            address = 0;

        } catch (@NotNull IOException e) {
            Jvm.warn().on(OS.class, "Error on unmap and release", e);
        }
    }
}
