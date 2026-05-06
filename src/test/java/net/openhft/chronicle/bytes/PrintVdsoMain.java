/*
 * Copyright 2013-2025 chronicle.software; SPDX-License-Identifier: Apache-2.0
 */
package net.openhft.chronicle.bytes;

import org.jetbrains.annotations.NotNull;

import java.io.*;

public class PrintVdsoMain {
    public static void main(String[] args)
            throws IOException, IllegalStateException {
        long start = 0;
        long end = 0;
        @NotNull String maps = "/proc/self/maps";
        if (!new File(maps).exists()) return;
        try (@NotNull BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(maps)))) {
            for (String line; (line = br.readLine()) != null; ) {
                if (line.endsWith("[vdso]")) {
                    @NotNull String[] parts = line.split("[- ]");
                    start = Long.parseLong(parts[0], 16);
                    end = Long.parseLong(parts[1], 16);
                }

//                System.out.println(line);
            }
        } catch (IOException ioe) {
            throw ioe;
        }
        System.out.printf("vdso %x to %x %n", start, end);
        @NotNull PointerBytesStore nb = new PointerBytesStore();
        nb.set(start, end - start);
        @NotNull FileOutputStream fos = new FileOutputStream("vdso.elf");
        for (Bytes<?> b = nb.bytesForRead(); b.readRemaining() > 0; )
            fos.write(b.readByte());
        fos.close();
    }
}
