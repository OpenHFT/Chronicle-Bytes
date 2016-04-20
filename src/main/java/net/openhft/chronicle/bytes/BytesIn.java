package net.openhft.chronicle.bytes;

/**
 * Created by Peter on 20/04/2016.
 */
public interface BytesIn<Underlying> extends
        StreamingDataInput<Bytes<Underlying>>,
        ByteStringParser<Bytes<Underlying>> {
}
