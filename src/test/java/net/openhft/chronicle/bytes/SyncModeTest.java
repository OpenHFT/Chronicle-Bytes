package net.openhft.chronicle.bytes;

import net.openhft.chronicle.core.io.IOTools;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.stream.Stream;

import static org.junit.Assert.assertEquals;

@RunWith(Parameterized.class)
public class SyncModeTest {
    private final SyncMode syncMode;

    public SyncModeTest(SyncMode syncMode) {
        this.syncMode = syncMode;
    }

    @Parameterized.Parameters(name = "{0}")
    public static Object[][] parameters() {
        return Stream.of(SyncMode.values()).map(s -> new Object[]{s}).toArray(Object[][]::new);
    }

    @Test
    public void largeFile() throws FileNotFoundException {
        File tmpfile = IOTools.createTempFile("sync.dat");
        try (MappedFile mappedFile = MappedFile.mappedFile(tmpfile, 64 << 20);
             MappedBytes bytes = MappedBytes.mappedBytes(mappedFile)) {
            mappedFile.syncMode(syncMode);
            bytes.readLong(0);
            MappedBytesStore mbs = (MappedBytesStore) (BytesStore) bytes.bytesStore;
            assertEquals(syncMode, mbs.syncMode());
            for (int i = 0; i < 64 << 20; i += 1 << 20) {
                mbs.syncUpTo(i);
                for (int j = 0; j < 1 << 20; j += 4 << 10)
                    bytes.writeLong(i + j, j);
            }
        }
    }
}
