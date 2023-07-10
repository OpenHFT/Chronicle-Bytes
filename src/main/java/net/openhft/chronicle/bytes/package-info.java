/**
 * The Chronicle Bytes package provides low-level memory access wrappers with functionalities
 * akin to Java NIO's ByteBuffer. The API supports UTF-8 and ISO-8859-1 encoded strings,
 * thread-safe off-heap memory operations, deterministic release of resources via reference
 * counting, and more.
 * <p>
 * The main classes in this package are:
 * <p>
 * {@link net.openhft.chronicle.bytes.Bytes}: A class for managing byte arrays, including
 * operations for reading and writing data. It extends if you write data into it which is
 * larger than its real capacity. It provides access to the cursors for reading and writing
 * data at desired indices.
 * <p>
 * {@link net.openhft.chronicle.bytes.BytesStore}: A block of memory with fixed size into
 * which you can write data and later read. You cannot use the cursors with a BytesStore,
 * unlike Bytes.
 * <p>
 * {@link net.openhft.chronicle.bytes.BytesIn}: Interface that provides a range of methods
 * for reading data from bytes.
 * <p>
 * {@link net.openhft.chronicle.bytes.BytesOut}: Interface that provides a range of methods
 * for writing data to bytes.
 * <p>
 * {@link net.openhft.chronicle.bytes.ByteStringParser}: An interface for parsing byte strings.
 * <p>
 * {@link net.openhft.chronicle.bytes.ByteStringAppender}: An interface for appending byte strings.
 * <p>
 * {@link net.openhft.chronicle.bytes.RandomDataInput}: An interface that extends the {@link net.openhft.chronicle.bytes.StreamingDataInput}.
 * <p>
 * {@link net.openhft.chronicle.bytes.RandomDataOutput}: An interface that extends the {@link net.openhft.chronicle.bytes.StreamingDataOutput}.
 * <p>
 * {@link net.openhft.chronicle.bytes.StreamingDataInput}: An interface for reading data from a stream.
 * <p>
 * {@link net.openhft.chronicle.bytes.StreamingDataOutput}: An interface for writing data to a stream.
 * <p>
 * For more information, please refer to each class documentation.
 */
package net.openhft.chronicle.bytes;
