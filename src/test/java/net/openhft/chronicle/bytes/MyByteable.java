package net.openhft.chronicle.bytes;

import org.jetbrains.annotations.NotNull;

class MyByteable implements BytesMarshallable {
    boolean flag;
    byte b;
    short s;
    char c;
    int i;
    float f;
    long l;
    double d;

    public MyByteable() {
    }

    public MyByteable(boolean flag, byte b, short s, char c, int i, float f, long l, double d) {
        this.flag = flag;
        this.b = b;
        this.s = s;
        this.c = c;
        this.i = i;
        this.f = f;
        this.l = l;
        this.d = d;
    }

    @NotNull
    @Override
    public String toString() {
        return "MyByteable{" +
                "flag=" + flag +
                ", b=" + b +
                ", s=" + s +
                ", c=" + c +
                ", i=" + i +
                ", f=" + f +
                ", l=" + l +
                ", d=" + d +
                '}';
    }
}
