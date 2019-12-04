package net.openhft.chronicle.bytes;

public interface CommonMarshallable {
    /**
     * @return whether this message should be written as self describing
     */
    default boolean usesSelfDescribingMessage() {
        return false;
    }
}
