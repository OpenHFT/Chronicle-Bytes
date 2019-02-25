package net.openhft.chronicle.bytes.cooler;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.core.cooler.CoolerTester;
import net.openhft.chronicle.core.cooler.CpuCoolers;
import org.jetbrains.annotations.NotNull;

public class BytesCoolerMain {
    static Bytes[] bytesArray = new Bytes[1000000];

    public static void main(String[] args) {
        for (int i = 0; i < bytesArray.length; i++) {
            bytesArray[i] = Bytes.allocateElasticDirect();
        }
        System.out.println("BEST CASE");
        {
            CoolerTester dt = new CoolerTester(() -> doTest(bytesArray[0]),
                    CpuCoolers.BUSY1);
            dt.repeat(5);
            dt.run();

        }
        System.out.println("\nWITH COOLERS ACROSS MEMORY");
        int[] i = {0};
        CoolerTester dt = new CoolerTester(() -> doTest(bytesArray[i[0]++ % bytesArray.length]),
                CpuCoolers.BUSY1,
                CpuCoolers.BUSY300,
                CpuCoolers.BUSY1000,
                CpuCoolers.PAUSE1,
                CpuCoolers.PAUSE10,
                CpuCoolers.PAUSE100,
                CpuCoolers.SERIALIZATION,
                CpuCoolers.MEMORY_COPY,
                CpuCoolers.ALL
        );
        dt.repeat(5);
        dt.run();
    }

    @NotNull
    public static Object doTest(Bytes bytes) {
        bytes.clear();
        bytes.append(Math.PI);
        double v = bytes.parseDouble();
        if (v < 0) throw new AssertionError();
        return bytes;
    }
}
