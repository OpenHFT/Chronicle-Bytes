/*
 * Copyright (c) 2016-2022 chronicle.software
 *
 *     https://chronicle.software
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
