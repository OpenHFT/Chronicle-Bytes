/*
 * Copyright 2016 higherfrequencytrading.com
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

import java.io.*;

/**
 * Created by peter on 12/07/15.
 */
public class PrintVdsoMain {
    public static void main(String[] args) throws IOException, IllegalStateException {
        long start = 0;
        long end = 0;
        String maps = "/proc/self/maps";
        if (!new File(maps).exists()) return;
        BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(maps)));
        try {
            for (String line; (line = br.readLine()) != null; ) {
                if (line.endsWith("[vdso]")) {
                    String[] parts = line.split("[- ]");
                    start = Long.parseLong(parts[0], 16);
                    end = Long.parseLong(parts[1], 16);
                }

//                System.out.println(line);
            }
        } catch (IOException ioe) {
            br.close();
            throw ioe;
        }
        System.out.printf("vdso %x to %x %n", start, end);
        PointerBytesStore nb = new PointerBytesStore();
        nb.set(start, end - start);
        FileOutputStream fos = new FileOutputStream("vdso.elf");
        for (Bytes b = nb.bytesForRead(); b.readRemaining() > 0; )
            fos.write(b.readByte());
        fos.close();
    }
}
