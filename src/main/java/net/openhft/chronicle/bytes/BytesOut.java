package net.openhft.chronicle.bytes;

/**
 * Created by Peter on 20/04/2016.
 */
public interface BytesOut<Underlying> extends
        StreamingDataOutput<Bytes<Underlying>>,
        ByteStringAppender<Bytes<Underlying>>,
        BytesPrepender<Bytes<Underlying>> {
}
