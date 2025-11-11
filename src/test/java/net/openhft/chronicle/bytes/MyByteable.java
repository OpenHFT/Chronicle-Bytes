/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes;

import net.openhft.chronicle.core.io.InvalidMarshallableException;
import net.openhft.chronicle.core.io.Validatable;
import org.jetbrains.annotations.NotNull;

class MyByteable implements BytesMarshallable, Validatable {
    private boolean flag;
    byte b;
    short s;
    private char c;
    private int i;
    private float f;
    private long l;
    private double d;

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

    @Override
    public void validate() throws InvalidMarshallableException {
        if (b == 0)
            throw new InvalidMarshallableException("b must not be 0");
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
