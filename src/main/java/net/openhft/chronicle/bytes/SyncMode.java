/*
 * Copyright (c) 2016-2022 chronicle.software
 *
 *     https://chronicle.software
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
 * An enumeration of sync modes for disk operations.
 * This enum controls whether the write operations are synchronized with the disk, and if so, whether
 * to wait for the synchronization to complete before continuing with the program execution.
 */
public enum SyncMode {
    /**
     * No synchronization is performed. The write operations are not explicitly synced with the disk.
     */
    NONE(null),
    /**
     * Synchronization is performed, and the program waits for the sync operation to complete before proceeding.
     * This mode guarantees that the write operation is completed before the next operation is carried out.
     * However, this mode may not be supported on all platforms.
     */
    SYNC(MSyncFlag.MS_SYNC),
    /**
     * Synchronization is scheduled but the program does not wait for it to complete before proceeding.
     * This mode allows the write operation to be carried out asynchronously. The actual write to the disk
     * happens later and does not block the program execution. However, this mode may not be supported on all platforms.
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
