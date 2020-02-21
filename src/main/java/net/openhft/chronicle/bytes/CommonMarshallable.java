package net.openhft.chronicle.bytes;

import net.openhft.chronicle.core.annotation.DontChain;

@DontChain
public interface CommonMarshallable {
    /**
     * @return whether this message should be written as self describing
     */
    default boolean usesSelfDescribingMessage() {
        return false;
    }
}
