package net.openhft.chronicle.bytes;

import net.openhft.posix.MSyncFlag;

public enum SyncMode {
    NONE(null),
    SYNC(MSyncFlag.MS_SYNC),
    ASYNC(MSyncFlag.MS_ASYNC);

    private final MSyncFlag mSyncFlag;

    SyncMode(MSyncFlag mSyncFlag) {
        this.mSyncFlag = mSyncFlag;
    }

    public MSyncFlag mSyncFlag() {
        return mSyncFlag;
    }
}
