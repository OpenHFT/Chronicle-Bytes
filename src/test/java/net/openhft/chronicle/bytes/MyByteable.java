/*
 * Copyright (c) 2016-2022 chronicle.software
 *
 *     https://chronicle.software
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.openhft.chronicle.bytes;

import net.openhft.chronicle.core.io.InvalidMarshallableException;
import net.openhft.chronicle.core.io.Validatable;
import org.jetbrains.annotations.NotNull;

class MyByteable implements BytesMarshallable, Validatable {
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
