package net.openhft.chronicle.bytes;

import org.jetbrains.annotations.NotNull;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.util.UUID;

class MyScalars implements BytesMarshallable {
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

    @NotNull
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
