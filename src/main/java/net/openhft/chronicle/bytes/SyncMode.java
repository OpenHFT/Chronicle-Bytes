/*
 * Copyright 2016-2025 chronicle.software
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
