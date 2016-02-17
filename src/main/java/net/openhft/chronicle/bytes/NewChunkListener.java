package net.openhft.chronicle.bytes;

/**
 * Created by peter on 12/02/2016.
 */
@FunctionalInterface
public interface NewChunkListener {
    void onNewChunk(String filename, int chunk, long delayMicros);
}
