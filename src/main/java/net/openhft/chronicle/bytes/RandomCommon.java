package net.openhft.chronicle.bytes;

import java.nio.ByteOrder;

public interface RandomCommon<S extends RandomCommon<S>> {
    /**
     * @return the highest offset or position allowed for this buffer.
     */
    default long limit() {
        return capacity();
    }

    default long readLimit() {
        return Math.min(realCapacity(), limit());
    }

    /**
     * @return the actual amount of data which can be read.
     */
    long realCapacity();

    /**
     * @return the highest limit allowed for this buffer.
     */
    long capacity();

    /**
     * Obtain the underlying address.  This is for expert users only.
     *
     * @return the underlying address of the buffer
     * @throws UnsupportedOperationException if the underlying buffer is on the heap
     */
    long address() throws UnsupportedOperationException;

    boolean isNative();

    default ByteOrder byteOrder() {
        return ByteOrder.nativeOrder();
    }
}
