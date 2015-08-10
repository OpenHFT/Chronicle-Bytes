# Chronicle Bytes

Chronicle Bytes contains all the low level memory access wrappers.  It is built on Chronicle Core's direct memory and OS system call access.

Chronicle Bytes has a similar purpose to Java NIO's ByteBuffer with some extensions.

The API supports.

 - 64-bit sizes
 - UTF-8 and ISO-8859-1 encoded strings.
 - thread safe off heap memory operations.
 - deterministic release of resources via reference counting.
 - compressed data types such as stop bit encoding.
 - elastic ByteBuffer wrappers which resize as required.
 - parsing text and writing text directly to off heap bytes.

