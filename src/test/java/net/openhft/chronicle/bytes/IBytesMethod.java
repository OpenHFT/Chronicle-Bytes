package net.openhft.chronicle.bytes;

interface IBytesMethod {
    @MethodId(0x81L)
    void myByteable(MyByteable byteable);

    @MethodId(0x82L)
    void myScalars(MyScalars scalars);

    @MethodId(0x83L)
    void myNested(MyNested nested);
}
