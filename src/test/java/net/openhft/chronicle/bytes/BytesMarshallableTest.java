package net.openhft.chronicle.bytes;

import net.openhft.chronicle.core.io.IORuntimeException;
import org.junit.Test;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.util.UUID;

import static org.junit.Assert.assertEquals;

/**
 * Created by Peter on 20/04/2016.
 */
public class BytesMarshallableTest {
    @Test
    public void serializePrimitives() throws IORuntimeException {
        Bytes bytes = Bytes.elasticByteBuffer();
        MyByteable mb1 = new MyByteable(false, (byte) 1, (short) 2, '3', 4, 5.5f, 6, 7.7);
        MyByteable mb2 = new MyByteable(true, (byte) 11, (short) 22, 'T', 44, 5.555f, 66, 77.77);
        mb1.writeMarshallable(bytes);
        mb2.writeMarshallable(bytes);

        MyByteable mb3 = new MyByteable();
        MyByteable mb4 = new MyByteable();
        mb3.readMarshallable(bytes);
        mb4.readMarshallable(bytes);

        assertEquals(mb1.toString(), mb3.toString());
        assertEquals(mb2.toString(), mb4.toString());
    }

    @Test
    public void serializeScalars() throws IORuntimeException {
        Bytes bytes = Bytes.elasticByteBuffer();
        MyScalars mb1 = new MyScalars("Hello", BigInteger.ONE, BigDecimal.TEN, LocalDate.now(), LocalTime.now(), LocalDateTime.now(), ZonedDateTime.now(), UUID.randomUUID());
        MyScalars mb2 = new MyScalars("World", BigInteger.ZERO, BigDecimal.ZERO, LocalDate.now(), LocalTime.now(), LocalDateTime.now(), ZonedDateTime.now(), UUID.randomUUID());
        mb1.writeMarshallable(bytes);
        mb2.writeMarshallable(bytes);

        MyScalars mb3 = new MyScalars();
        MyScalars mb4 = new MyScalars();
        mb3.readMarshallable(bytes);
        mb4.readMarshallable(bytes);

        assertEquals(mb1.toString(), mb3.toString());
        assertEquals(mb2.toString(), mb4.toString());
    }

    @Test
    public void serializeNested() throws IORuntimeException {
        Bytes bytes = Bytes.elasticByteBuffer();
        MyByteable mb1 = new MyByteable(false, (byte) 1, (short) 2, '3', 4, 5.5f, 6, 7.7);
        MyByteable mb2 = new MyByteable(true, (byte) 11, (short) 22, 'T', 44, 5.555f, 66, 77.77);
        MyScalars ms1 = new MyScalars("Hello", BigInteger.ONE, BigDecimal.TEN, LocalDate.now(), LocalTime.now(), LocalDateTime.now(), ZonedDateTime.now(), UUID.randomUUID());
        MyScalars ms2 = new MyScalars("World", BigInteger.ZERO, BigDecimal.ZERO, LocalDate.now(), LocalTime.now(), LocalDateTime.now(), ZonedDateTime.now(), UUID.randomUUID());
        MyNested mn1 = new MyNested(mb1, ms1);
        MyNested mn2 = new MyNested(mb2, ms2);
        mn1.writeMarshallable(bytes);
        mn2.writeMarshallable(bytes);

        MyNested mn3 = new MyNested();
        MyNested mn4 = new MyNested();
        mn3.readMarshallable(bytes);
        mn4.readMarshallable(bytes);

        assertEquals(mn1.toString(), mn3.toString());
        assertEquals(mn2.toString(), mn4.toString());
    }

    static class MyByteable implements BytesMarshallable {
        boolean flag;
        byte b;
        short s;
        char c;
        int i;
        float f;
        long l;
        double d;

        public MyByteable() {
        }

        public MyByteable(boolean flag, byte b, short s, char c, int i, float f, long l, double d) {
            this.flag = flag;
            this.b = b;
            this.s = s;
            this.c = c;
            this.i = i;
            this.f = f;
            this.l = l;
            this.d = d;
        }

        @Override
        public String toString() {
            return "MyByteable{" +
                    "flag=" + flag +
                    ", b=" + b +
                    ", s=" + s +
                    ", c=" + c +
                    ", i=" + i +
                    ", f=" + f +
                    ", l=" + l +
                    ", d=" + d +
                    '}';
        }
    }

    static class MyScalars implements BytesMarshallable {
        String s;
        BigInteger bi;
        BigDecimal bd;
        LocalDate date;
        LocalTime time;
        LocalDateTime dateTime;
        ZonedDateTime zonedDateTime;
        UUID uuid;

        public MyScalars() {
        }

        public MyScalars(String s, BigInteger bi, BigDecimal bd, LocalDate date, LocalTime time, LocalDateTime dateTime, ZonedDateTime zonedDateTime, UUID uuid) {
            this.s = s;
            this.bi = bi;
            this.bd = bd;
            this.date = date;
            this.time = time;
            this.dateTime = dateTime;
            this.zonedDateTime = zonedDateTime;
            this.uuid = uuid;
        }

        @Override
        public String toString() {
            return "MyScalars{" +
                    "s='" + s + '\'' +
                    ", bi=" + bi +
                    ", bd=" + bd +
                    ", date=" + date +
                    ", time=" + time +
                    ", dateTime=" + dateTime +
                    ", zonedDateTime=" + zonedDateTime +
                    ", uuid=" + uuid +
                    '}';
        }
    }

    static class MyNested implements BytesMarshallable {
        MyByteable byteable;
        MyScalars scalars;

        public MyNested() {
        }

        public MyNested(MyByteable byteable, MyScalars scalars) {
            this.byteable = byteable;
            this.scalars = scalars;
        }

        @Override
        public String toString() {
            return "MyNested{" +
                    "byteable=" + byteable +
                    ", scalars=" + scalars +
                    '}';
        }
    }


}
