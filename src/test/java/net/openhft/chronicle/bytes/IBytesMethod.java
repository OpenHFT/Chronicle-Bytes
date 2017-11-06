package net.openhft.chronicle.bytes;

interface IBytesMethod {
    @MethodId(1L)
    void myByteable(MyByteable byteable);

    @MethodId(2L)
    void myScalars(MyScalars scalars);

    @MethodId(3L)
    void myNested(MyNested nested);
}
