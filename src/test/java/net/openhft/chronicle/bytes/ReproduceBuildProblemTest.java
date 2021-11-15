package net.openhft.chronicle.bytes;

import org.junit.jupiter.api.Test;

import static net.openhft.chronicle.bytes.BytesFactoryUtil.*;
import static org.junit.jupiter.api.Assertions.fail;

final class ReproduceBuildProblemTest extends BytesTestCommon {

    @Test
    void reproduce() {

        BytesFactoryUtil.provideBytesObjects()
                .forEach(args -> {
                    final Bytes<Object> bytes = bytes(args);
                    try {
                        System.out.println("Using " + createCommand(args) + " -> " + bytes.getClass().getName());
                        System.out.flush();
                        System.err.flush();
                        if (isReadWrite(args)) {
                            bytes.write("A");
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        System.out.flush();
                        System.err.flush();
                        fail(e);
                    } finally {
                        bytes.releaseLast();
                    }

                });

    }

}