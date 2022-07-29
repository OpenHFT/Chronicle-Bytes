/*
 * Copyright (c) 2016-2022 chronicle.software
 *
 * https://chronicle.software
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

import net.openhft.chronicle.core.Jvm;
import org.junit.Test;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

public class SequentialWrapForWriteTest {
    public void runner() {
        ByteBuffer outboundApplicationData = ByteBuffer.allocateDirect(16704);

        int counter = 0;
        for (int i = 0; i < 500; i++) {
            Bytes<ByteBuffer> out = Bytes.wrapForWrite(outboundApplicationData);
            try {
                final String payload = "ping-" + (counter - 1);
                if (out.writeRemaining() >= (16L + payload.length())) {
                    System.err.println("Writing at " + out.writePosition() + " remaining " + out.writeRemaining());
                    out.writeInt(0xFEDCBA98);
                    out.writeLong((counter++));
                    out.writeInt(payload.length());
                    out.write(payload.getBytes(StandardCharsets.US_ASCII));
                }

                outboundApplicationData.position((int) out.writePosition());
            } finally {
                out.releaseLast();
            }

            Jvm.pause(10);
        }
    }

    @Test
    public void test() throws InterruptedException {
        Thread[] threads = new Thread[2];
        for (int i = 0; i < threads.length; i++) {
            threads[i] = new Thread(this::runner);

            threads[i].start();
        }

        for (Thread thread : threads)
            thread.join();
    }
}
