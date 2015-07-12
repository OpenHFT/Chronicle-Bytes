/*
 *     Copyright (C) 2015  higherfrequencytrading.com
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU Lesser General Public License as published by
 *     the Free Software Foundation, either version 3 of the License.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU Lesser General Public License for more details.
 *
 *     You should have received a copy of the GNU Lesser General Public License
 *     along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package net.openhft.chronicle.bytes;

import java.io.*;

/**
 * Created by peter on 12/07/15.
 */
public class PrintVdsoMain {
    public static void main(String[] args) throws IOException, InterruptedException {
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
