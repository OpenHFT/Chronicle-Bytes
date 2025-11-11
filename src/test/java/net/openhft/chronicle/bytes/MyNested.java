/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes;

import net.openhft.chronicle.core.io.InvalidMarshallableException;
import net.openhft.chronicle.core.io.Validatable;
import org.jetbrains.annotations.NotNull;

class MyNested implements BytesMarshallable, Validatable {
    private MyByteable byteable;
    private MyScalars scalars;

    public MyNested() {
    }

    public MyNested(MyByteable byteable, MyScalars scalars) {
        this.byteable = byteable;
        this.scalars = scalars;
    }

    @NotNull
    @Override
    public String toString() {
        return "MyNested{" +
                "byteable=" + byteable +
                ", scalars=" + scalars +
                '}';
    }

    @Override
    public void validate() throws InvalidMarshallableException {
        byteable.validate();
    }
}
