package net.openhft.chronicle.bytes.util;

import net.openhft.chronicle.bytes.Bytes;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.BufferOverflowException;
import java.nio.BufferUnderflowException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.security.SecureRandom;

@Deprecated(/* to be remoed in x.22 */)
public class BigCopyMain {

    public static void main(String[] args)
            throws IOException, IllegalArgumentException, IllegalStateException, BufferOverflowException, BufferUnderflowException {
        int initialCapacity = 10 * 1024 * 1024;
        long fileSize = 5368709120l;
        byte[] buffer = new byte[initialCapacity];
        new SecureRandom().nextBytes(buffer);

        Bytes bytes = Bytes.allocateElasticDirect(initialCapacity);
        while (bytes.writePosition() < fileSize) {
            bytes.write(buffer);
        }
        System.out.println("Writing file 1");
        Path path = Paths.get("./textFile1.bin");
        try (OutputStream outputStream = Files.newOutputStream(path, StandardOpenOption.CREATE_NEW)) {
            while (bytes.read(buffer) > 0) {
                outputStream.write(buffer);
            }
        }
        long result = path.toFile().length();
        if (fileSize != result) {
            throw new RuntimeException(String.format("Expecting %s but file size is %s", fileSize, result));
        }

        bytes = Bytes.allocateElasticDirect(initialCapacity);
        new SecureRandom().nextBytes(buffer);
        while (bytes.writePosition() < fileSize) {
            bytes.write(buffer);
        }
        path = Paths.get("./textFile2.bin");
        System.out.println("Writing file 2");
        // crashing...
        try (OutputStream outputStream = Files.newOutputStream(path, StandardOpenOption.CREATE_NEW)) {
            bytes.copyTo(outputStream);
        }
        result = path.toFile().length();
        if (fileSize != result) {
            throw new RuntimeException(String.format("Expecting %s but file size is %s", fileSize, result));
        }
    }
}