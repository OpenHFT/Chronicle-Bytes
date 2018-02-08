package net.openhft.chronicle.bytes.ref;

import net.openhft.chronicle.bytes.BytesStore;

final class StoreRef
{
    BytesStore b;

    void clean() {
        if (b != null && b.refCount() != 0) {
            b.release();
        }
    }
}
