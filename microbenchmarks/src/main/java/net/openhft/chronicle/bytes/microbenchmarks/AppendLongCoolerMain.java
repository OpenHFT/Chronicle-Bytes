package net.openhft.chronicle.bytes.microbenchmarks;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.core.cooler.CoolerTester;
import net.openhft.chronicle.core.cooler.CpuCoolers;
import org.jetbrains.annotations.NotNull;

public class AppendLongCoolerMain {

    static int i = 0;
    static long[] longs = {Integer.MIN_VALUE, -128, 0, 1, 11, 111, Integer.MAX_VALUE, Long.MAX_VALUE};

    public static void main(String[] args) {
        Bytes bytes = Bytes.allocateElasticDirect(32);

        new CoolerTester(
                CpuCoolers.PAUSE1,
                CpuCoolers.BUSY100,
                CpuCoolers.ALL
        ).add("write", () -> doWrite(bytes))
                .run();
    }

    @NotNull
    public static Object doWrite(Bytes bytes) {
        bytes.clear();
        bytes.append(longs[i++ % longs.length]);
        return bytes;
    }

    @NotNull
    public static Object doRead(Bytes bytes) {
        bytes.readPosition(0);
        double v = bytes.parseDouble();
        if (v < 0) throw new AssertionError();
        return bytes;
    }

    @NotNull
    public static Object doTest(Bytes bytes) {
        doWrite(bytes);
        doRead(bytes);
        return bytes;
    }
}
