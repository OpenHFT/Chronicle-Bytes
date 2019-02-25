package net.openhft.chronicle.bytes.cooler;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.core.cooler.CoolerTester;
import net.openhft.chronicle.core.cooler.CpuCooler;
import net.openhft.chronicle.core.cooler.CpuCoolers;
import org.jetbrains.annotations.NotNull;

public class BytesCoolerMain {
    static Bytes[] bytesArray = new Bytes[8 << 10];

    public static void main(String[] args) {
        for (int i = 0; i < bytesArray.length; i++) {
            bytesArray[i] = Bytes.allocateElasticDirect();
        }
        System.out.println("BEST CASE");
        new CoolerTester(new CpuCooler[]{CpuCoolers.BUSY})
                .add("write", () -> doWrite(bytesArray[0]))
                .add("read", () -> doRead(bytesArray[0]))
                .add("both", () -> doTest(bytesArray[0]))
                .repeat(5)
                .run();

        System.out.println("\nWITH COOLERS ACROSS MEMORY");
        int[] i = {0};
        new CoolerTester(
                CpuCoolers.BUSY,
                CpuCoolers.BUSY300,
//                CpuCoolers.BUSY1000,
                CpuCoolers.PAUSE1,
                CpuCoolers.PAUSE10,
//                CpuCoolers.PAUSE100,
//                CpuCoolers.SERIALIZATION,
//                CpuCoolers.MEMORY_COPY,
                CpuCoolers.ALL
        ).add("write", () -> doWrite(
                bytesArray[++i[0] & (bytesArray.length - 1)]
        )).add("read", () -> doRead(
                bytesArray[++i[0] & (bytesArray.length - 1)]
        )).add("both", () -> doTest(
                bytesArray[++i[0] & (bytesArray.length - 1)]
        )).run();
    }

    @NotNull
    public static Object doWrite(Bytes bytes) {
        bytes.clear();
        bytes.append(Math.PI);
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
