package net.openhft.chronicle.bytes.render.docExamples;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.bytes.render.Decimaliser;
import net.openhft.chronicle.bytes.render.DecimalAppender;
import net.openhft.chronicle.bytes.render.StandardDecimaliser;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DecimaliserDocTest {
    @Test
    void exampleUsage() {
        Bytes<?> bytes = Bytes.allocateElasticOnHeap();
        Decimaliser d = StandardDecimaliser.STANDARD;
        boolean ok = d.toDecimal(1.2345,
                (neg, m, e) -> bytes.append(neg ? '-' : '')
                        .append(m)
                        .append('e')
                        .append(-e));
        assertTrue(ok);
        assertEquals("12345e-4", bytes.toString());
    }
}
