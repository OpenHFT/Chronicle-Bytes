package net.openhft.chronicle.bytes;

import net.openhft.chronicle.core.ReferenceCounted;

/**
 * Created by peter.lawrey on 21/09/2015.
 */
public interface MappedBytesStoreFactory {
    MappedBytesStore create(ReferenceCounted owner, long start, long address, long capacity, long safeCapacity);
}
