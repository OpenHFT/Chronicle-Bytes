package net.openhft.chronicle.bytes;

import net.openhft.chronicle.core.io.Closeable;

/**
 * Created by Rob Austin
 */
public interface BytesContext extends Closeable {

    /**
     * @return a bytes to write to
     */
    @SuppressWarnings("rawtypes")
    Bytes bytes();

    /**
     * @return the key to be written to
     */
    int key();

}
