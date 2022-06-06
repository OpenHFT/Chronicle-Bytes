package net.openhft.chronicle.bytes.internal;

import net.openhft.chronicle.bytes.Bytes;
import org.junit.Assert;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BytesInternalTest {

    @Test
    void testContentEqual() {

        Bytes a = Bytes.allocateElasticDirect();
        Bytes b = Bytes.allocateElasticDirect();

        a.append("hello world");
        b.append("hello world");

        Assert.assertTrue(a.contentEquals(b));
    }


    @Test
    void testContentNotEqualButSameLen() {

        Bytes a = Bytes.allocateElasticDirect();
        Bytes b = Bytes.allocateElasticDirect();


        a.append("hello world1");
        b.append("hello world2");

        Assert.assertFalse(a.contentEquals(b));
    }

    @Test
    void testContentNotEqualButDiffLen() {

        Bytes a = Bytes.allocateElasticDirect();
        Bytes b = Bytes.allocateElasticDirect();


        a.append("hello world");
        b.append("hello world2");

        Assert.assertFalse(a.contentEquals(b));
    }

}