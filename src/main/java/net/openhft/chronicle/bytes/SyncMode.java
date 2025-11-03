/*
 * Copyright 2016-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes;

import net.openhft.posix.MSyncFlag;

/**
 * Synchronisation options for memory mapped file updates, mirroring the
 * behaviour of {@code msync(2)}.
 */
public enum SyncMode {
    /**
     * No synchronisation is requested.
     */
    NONE(null),
    /**
     * Synchronous update using {@link MSyncFlag#MS_SYNC}. The call blocks until
     * all modified pages are written to disk.
     */
    SYNC(MSyncFlag.MS_SYNC),
    /**
     * Asynchronous update using {@link MSyncFlag#MS_ASYNC}. Dirty pages are
     * scheduled for write out but the call returns immediately.
     */
    ASYNC(MSyncFlag.MS_ASYNC);

    private final MSyncFlag mSyncFlag;

    SyncMode(MSyncFlag mSyncFlag) {
        this.mSyncFlag = mSyncFlag;
    }

    /**
     * Returns the {@link MSyncFlag} associated with this sync mode.
     *
     * @return the MSyncFlag value, may be null for {@code NONE}
     */
    public MSyncFlag mSyncFlag() {
        return mSyncFlag;
    }
}
