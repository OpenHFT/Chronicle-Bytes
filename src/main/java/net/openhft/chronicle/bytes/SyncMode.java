/*
 * Copyright (c) 2016-2022 chronicle.software
 *
 *       https://chronicle.software
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

public enum SyncMode {
    /**
     * No sync is performed on any platform
     */
    NONE(null),
    /**
     * Wait for a sync to disk to be performed, if the platform supports this
     */
    SYNC(MSyncFlag.MS_SYNC),
    /**
     * Schedule a sync to disk to be performed, but don't wait for it, if the platform supports this
     */
    ASYNC(MSyncFlag.MS_ASYNC);

    private final MSyncFlag mSyncFlag;

    SyncMode(MSyncFlag mSyncFlag) {
        this.mSyncFlag = mSyncFlag;
    }

    public MSyncFlag mSyncFlag() {
        return mSyncFlag;
    }
}
