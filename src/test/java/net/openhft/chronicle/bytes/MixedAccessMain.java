package net.openhft.chronicle.bytes;

import net.openhft.chronicle.core.OS;
import net.openhft.chronicle.core.io.ReferenceOwner;

import java.io.File;
import java.io.FileNotFoundException;

public class MixedAccessMain {
    public static void main(String[] args) throws FileNotFoundException {
        System.setProperty("jvm.resource.tracing", "true");
        File file = new File(OS.TMP + "/map-test");
        try (MappedBytes mf = MappedBytes.mappedBytes(file,
                1024 * 1024, OS.pageSize())) {
            mf.writeLong(0);
            BytesStore start = mf.bytesStore();
            start.reserve(ReferenceOwner.TMP);

            for (int i = 0; i < 1_000_000; i++) {
                mf.writeLong(i);
                start.writeLong(0, i);
            }
            start.release(ReferenceOwner.TMP);
        }
    }
}
