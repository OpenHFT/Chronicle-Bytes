package net.openhft.chronicle.bytes.cooler;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.core.cooler.CoolerTester;
import net.openhft.chronicle.core.cooler.CpuCoolers;
import org.jetbrains.annotations.NotNull;

public class BytesCoolerMain {

    public static void main(String[] args) {
        Bytes small = Bytes.allocateDirect(23);
        Bytes big = Bytes.allocateDirect(400);

        System.out.println("BEST CASE");

        System.out.println("\nWITH COOLERS ACROSS MEMORY");
        new CoolerTester(
                CpuCoolers.BUSY100,
                CpuCoolers.PAUSE1,
                CpuCoolers.ALL
        )
                .add("direct", () -> doWrite(big))
                .add("small", () -> doWrite(small))
                .run();
    }

    @NotNull
    public static Object doWrite(Bytes bytes) {
        bytes.clear();
        bytes.append(123.456);
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
