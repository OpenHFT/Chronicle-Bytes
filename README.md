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
 
# Comparison of access to native memory

| Access                                              | ByteBuffer       | Aeron UnsafeBuffer | Chronicle Bytes     |
|--------------------------------------------|:---------------:|:--------------------:|:--------------------:|
| Read/write primitives in native memory |  yes               |  yes                      |  yes                     |
| Separate Mutable interfaces                 | run time check |  yes                      |  yes                     |
| Read/Write UTF8 strings                      |  no                |  String                   |  any CharSequence + Appendable |
| Read/Write ISO-8859-1 strings             |  no                |  ?                         |  any CharSequence + Appendable |
| Support Endianness                            | Big and Little   |  Big and Little        | Native only           |
| Size of buffer                                     |  31-bit            |  31-bit                  | 63-bit                  |
| Elastic ByteBuffers                              |  no                 | no                        | yes                      |
| Disable bounds checks                        |  no                 | set globally           | by buffer              |
| Wrap an address                                 | no                 | yes                       | yes                      |
| Thread safe off heap operations            | no                  | int, long                | int, long, float and double |
| Streaming access                                | yes                 | no                        | yes                      |


