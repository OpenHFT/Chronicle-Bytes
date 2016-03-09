package net.openhft.chronicle.bytes;

public interface BytesConsumer {

    /**
     * Retrieves and removes the head of this queue, or returns {@code true} if this queue is
     * empty.
     *
     * @return false if this queue is empty
     */
    boolean read(Bytes<?> bytes);

    boolean isEmpty();
}