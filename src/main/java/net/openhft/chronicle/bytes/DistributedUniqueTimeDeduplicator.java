package net.openhft.chronicle.bytes;

public interface DistributedUniqueTimeDeduplicator {

    /**
     * Compare this new timestamp to the previously retained timstamp for the hostId
     *
     * @param timestampHostId to compare
     * @return -1 if the timestamp is older, 0 if the same, +1 if newer
     */
    int compareByHostId(long timestampHostId);

    /**
     * Compare this new timestamp to the previously retained timestamp for the hostId and retaining the timestamp
     *
     * @param timestampHostId to compare
     * @return -1 if the timestamp is older, 0 if the same, +1 if newer
     */
    int compareAndRetainNewer(long timestampHostId);
}
