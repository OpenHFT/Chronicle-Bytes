package net.openhft.chronicle.bytes.internal;

import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.core.io.IORuntimeException;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collection;

import static net.openhft.chronicle.bytes.Allocator.HEAP;
import static net.openhft.chronicle.bytes.Allocator.NATIVE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@RunWith(Parameterized.class)
public class BytesInternalTest {
    private final Bytes a;
    private final Bytes b;

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {Bytes.allocateElasticOnHeap(), Bytes.allocateElasticOnHeap()}
                , {Bytes.allocateElasticDirect(), Bytes.allocateElasticOnHeap()}
                , {Bytes.allocateElasticDirect(), Bytes.allocateElasticDirect()}
                , {Bytes.allocateElasticOnHeap(), Bytes.allocateElasticDirect()}
                , {Bytes.elasticByteBuffer(), Bytes.elasticByteBuffer()}
                , {Bytes.elasticHeapByteBuffer(), Bytes.elasticHeapByteBuffer()}
                , {Bytes.elasticHeapByteBuffer(), Bytes.elasticByteBuffer()}
        });
    }

    public BytesInternalTest(Bytes left, Bytes right) {
        this.a = left;
        this.b = right;
    }

    @Before
    public void before() {
        a.clear();
        b.clear();

    }

    @Test
    public void testContentEqual() {
        a.append("hello world");
        b.append("hello world");
        Assert.assertTrue(a.contentEquals(b));
    }

    @Test
    public void testContentNotEqualButSameLen() {
        a.append("hello world1");
        b.append("hello world2");
        Assert.assertFalse(a.contentEquals(b));
    }

    @Test
    public void testContentNotEqualButDiffLen() {
        a.append("hello world");
        b.append("hello world2");
        Assert.assertFalse(a.contentEquals(b));
    }


}