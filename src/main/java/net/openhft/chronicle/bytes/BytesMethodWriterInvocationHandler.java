package net.openhft.chronicle.bytes;

import net.openhft.chronicle.core.io.Closeable;

import java.lang.reflect.InvocationHandler;

public interface BytesMethodWriterInvocationHandler extends InvocationHandler {

    void onClose(Closeable closeable);
}
