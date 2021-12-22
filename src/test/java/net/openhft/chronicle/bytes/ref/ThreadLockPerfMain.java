package net.openhft.chronicle.bytes.ref;

import net.openhft.chronicle.bytes.MappedBytes;
import net.openhft.chronicle.core.Jvm;
import net.openhft.chronicle.core.OS;
import net.openhft.chronicle.core.threads.ThreadLock;
import net.openhft.posix.PosixAPI;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.stream.IntStream;

public class ThreadLockPerfMain {
    public static void main(String[] args) throws FileNotFoundException {

        File file = new File(OS.getTarget(), "ThreadLockPerfMain-" + System.nanoTime());
        try (MappedBytes mb = MappedBytes.singleMappedBytes(file, 64 << 10)) {
            final BinaryLongReference twoThreadId = new BinaryLongReference();
            twoThreadId.bytesStore(mb, 0, 8);
            ThreadLock tl = twoThreadId.vanillaThreadLock();
            for (int t = 1; t <= 12; t++) {
                int runs = 5_000_000;
                int threads = t;
                long start = System.nanoTime();
                IntStream.rangeClosed(1, threads)
                        .parallel()
                        .forEach(i -> {
                            int gettid = OS.isLinux() ? PosixAPI.posix().gettid() : i;
                            for (int j = 0; j < runs; j += threads) {
                                tl.lock(gettid);
                                tl.unlock(gettid);
                                Jvm.nanoPause();
                            }
                        });
                long time = System.nanoTime() - start;
                System.out.println("Threads: " + threads + ", avg latency " + time / runs + " ns");
            }
        }
    }
}