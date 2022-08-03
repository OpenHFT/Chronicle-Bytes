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

import net.openhft.affinity.Affinity;
import net.openhft.chronicle.core.Jvm;
import net.openhft.chronicle.core.io.IORuntimeException;
import org.junit.Test;

import java.nio.BufferOverflowException;
import java.nio.BufferUnderflowException;
import java.util.Comparator;
import java.util.function.Function;
import java.util.stream.Stream;

import static net.openhft.chronicle.core.UnsafeMemory.MEMORY;
import static org.junit.Assert.assertEquals;

public class StructTest extends BytesTestCommon {

    /**
     * Common base for structs to take care of initialisation and other boilerplating
     */
    static abstract class Struct<S extends Struct<S>> {
        protected Bytes<?> self;
        protected final Bytes<?> bytes;
        private final int size;
        protected long address;

        // return a default constructed standalone instance
        // (logically this is static, but then we can't force with abstract)
        protected abstract S construct(long address);

        /**
         * c++ new - construct with memory owned by self
         * @param size
         */
        protected Struct(int size) {
            this.size = size;
            bytes = new NativeBytes<>(new PointerBytesStore(), size);
            allocateAndInitialise();
        }

        /**
         * c++ placement new - construct at given address
         * @param size
         * @param address
         */
        protected Struct(int size, long address) {
            this.size = size;
            bytes = new NativeBytes<>(new PointerBytesStore(), size);
            initialise(address);
        }

        /**
         * return a new instance distinct copy of self - equivalent to c++ copy constructor
         * default is bitwise copy
         */
        public S copy() {
            S s = construct(0);
            s.copy((S)this);
            return s;
        }

        /**
         * return a new instance shared copy of self
         */
        public S share() {
            return construct(this.address);
        }

        /**
         * Replace self with a distinct copy of s - c++ operator=
         * @param s - the instance to copy
         * @return - self
         */
        protected S copy(S s) {
            if(self == null || s.address != this.address) {
                allocateAndInitialise();
                MEMORY.copyMemory(s.address, this.address, size);
            }
            return (S)this;
        }

        /**
         * Replace self with a shared copy of s - c++ pointer=
         * @param s - the instance to share
         * @return - self
         */
        protected S share(S s) {
            if(self != null || s.address != this.address) {
                deallocate();
                initialise(s.address);
            }

            return (S)this;
        }

        /**
         * Fully initialise self at given address
         * Override if struct contains any members which need specific initialisation
         * @param address
         */
        protected void initialise(final long address) {
            assert address != 0;

            ((PointerBytesStore) bytes.bytesStore()).set(address, size);
            bytes.readPosition(0);
            bytes.writePosition(size);
            this.address = bytes.addressForWrite(0);
        }

        /**
         * Get handle to underlying bytes
         * @return - the underlying bytes corresponding to this instance's members
         */
        public Bytes<?> bytes() { return bytes; }

        private void allocateAndInitialise() {
            if(self == null) {
                self = Bytes.allocateDirect(size);
                initialise(self.addressForWrite(0));
            }
        }

        private void deallocate() {
            if(self != null) {
                self.releaseLast();
                self = null;
            }
        }

        /**
        private <T extends Struct> void assertSameClass(T s) {
            if(!getClass().equals(s.getClass()))
                throw new IllegalArgumentException("Invalid assignment requested");
        }
         */
    }

    /**
     * Simple helper to wrap/cache a pointer to a Struct
     * @param <T> - the Struct type wrapped by the pointer
     */
    static class Pointer<T extends Struct<T>> {
        T ptr;
        Function<Long,T> supplier;
        long address;

        Pointer(Function<Long,T> supplier) {
            this.supplier = supplier;
        }

        void reset() { reset(0); }

        void reset(T t) { reset(t.address); }

        void reset(long address) {
            if(address == 0) {
                this.address = address;
                return;
            }

            if(this.address != address) {
                if (ptr == null)
                    ptr = supplier.apply(address);
                else
                    ptr.initialise(address);

                this.address = address;
            }
        }

        T get() {
            if(address == 0)
                return null;

            return ptr;
        }
    }

    @Test
    public void createStudents() {
        Student s3 = new Student()
                .gender(Gender.FEMALE)
                .name("Wonder Woman")
                .birth(1942, 1, 1)
                .grade(0, 0.95f);
        Student s2 = new Student()
                .gender(Gender.MALE)
                .name("Superman")
                .birth(1938, 4, 18)
                .grade(0, 0.96f);
        Student s1 = new Student()
                .gender(Gender.MALE)
                .name("The Phantom")
                .birth(1936, 2, 17)
                .grade(0, 0.97f);

        StringBuilder sb0 = new StringBuilder();
        sb0.append(s1.bytes().toHexString())
                .append(s2.bytes().toHexString())
                .append(s3.bytes().toHexString());
        System.out.println(sb0);

        if(Jvm.is64bit()) {
            assertEquals( "00000000 00 00 00 00 00 00 00 00  54 68 65 20 50 68 61 6e ········ The Phan\n" +
                    "00000010 74 6f 6d 00 00 00 00 00  00 00 00 00 00 00 00 00 tom····· ········\n" +
                    "00000020 00 00 00 00 00 00 00 00  00 00 00 00 00 00 00 00 ········ ········\n" +
                    "........\n" +
                    "00000040 00 00 00 00 00 00 00 00  90 07 02 11 ec 51 78 3f ········ ·····Qx?\n" +
                    "00000050 00 00 00 00 00 00 00 00  00 00 00 00 00 00 00 00 ········ ········\n" +
                    "........\n" +
                    "00000070 00 00 00 00 00 00 00 00  00 00 00 00             ········ ····    \n" +
                    "00000000 00 00 00 00 00 00 00 00  53 75 70 65 72 6d 61 6e ········ Superman\n" +
                    "00000010 00 00 00 00 00 00 00 00  00 00 00 00 00 00 00 00 ········ ········\n" +
                    "........\n" +
                    "00000040 00 00 00 00 00 00 00 00  92 07 04 12 8f c2 75 3f ········ ······u?\n" +
                    "00000050 00 00 00 00 00 00 00 00  00 00 00 00 00 00 00 00 ········ ········\n" +
                    "........\n" +
                    "00000070 00 00 00 00 00 00 00 00  00 00 00 00             ········ ····    \n" +
                    "00000000 00 00 00 00 01 00 00 00  57 6f 6e 64 65 72 20 57 ········ Wonder W\n" +
                    "00000010 6f 6d 61 6e 00 00 00 00  00 00 00 00 00 00 00 00 oman···· ········\n" +
                    "00000020 00 00 00 00 00 00 00 00  00 00 00 00 00 00 00 00 ········ ········\n" +
                    "........\n" +
                    "00000040 00 00 00 00 00 00 00 00  96 07 01 01 33 33 73 3f ········ ····33s?\n" +
                    "00000050 00 00 00 00 00 00 00 00  00 00 00 00 00 00 00 00 ········ ········\n" +
                    "........\n" +
                    "00000070 00 00 00 00 00 00 00 00  00 00 00 00             ········ ····    \n", sb0.toString());
        } else {
            assertEquals( "00000000 00 00 00 00 00 00 00 00  54 68 65 20 50 68 61 6e ········ The Phan\n" +
                    "00000010 74 6f 6d 00 00 00 00 00  00 00 00 00 00 00 00 00 tom····· ········\n" +
                    "00000020 00 00 00 00 00 00 00 00  00 00 00 00 00 00 00 00 ········ ········\n" +
                    "........\n" +
                    "00000040 00 00 00 00 00 00 00 00  90 07 02 11 ec 51 78 3f ········ ·····Qx?\n" +
                    "00000050 00 00 00 00 00 00 00 00  00 00 00 00 00 00 00 00 ········ ········\n" +
                    "........\n" +
                    "00000070 00 00 00 00 00 00 00 00                          ········         \n" +
                    "00000000 00 00 00 00 00 00 00 00  53 75 70 65 72 6d 61 6e ········ Superman\n" +
                    "00000010 00 00 00 00 00 00 00 00  00 00 00 00 00 00 00 00 ········ ········\n" +
                    "........\n" +
                    "00000040 00 00 00 00 00 00 00 00  92 07 04 12 8f c2 75 3f ········ ······u?\n" +
                    "00000050 00 00 00 00 00 00 00 00  00 00 00 00 00 00 00 00 ········ ········\n" +
                    "........\n" +
                    "00000070 00 00 00 00 00 00 00 00                          ········         \n" +
                    "00000000 00 00 00 00 01 00 00 00  57 6f 6e 64 65 72 20 57 ········ Wonder W\n" +
                    "00000010 6f 6d 61 6e 00 00 00 00  00 00 00 00 00 00 00 00 oman···· ········\n" +
                    "00000020 00 00 00 00 00 00 00 00  00 00 00 00 00 00 00 00 ········ ········\n" +
                    "........\n" +
                    "00000040 00 00 00 00 00 00 00 00  96 07 01 01 33 33 73 3f ········ ····33s?\n" +
                    "00000050 00 00 00 00 00 00 00 00  00 00 00 00 00 00 00 00 ········ ········\n" +
                    "........\n" +
                    "00000070 00 00 00 00 00 00 00 00                          ········         \n", sb0.toString());
        }

        // add links here (need the previous null addresses for the above output to be constant between runs)
        s1.next(s2);
        s2.next(s3);

        // walk the linked list
        StringBuilder sb = new StringBuilder();
        for( Student s = s1; s != null; s = s.next() ) {
            String line = s.name() + " " + s.gender() + ", born " + s.birth();
            sb.append(line).append("\n");
        }

        System.out.print(sb);
        assertEquals("" +
                        "The Phantom MALE, born 1936-2-17\n" +
                        "Superman MALE, born 1938-4-18\n" +
                        "Wonder Woman FEMALE, born 1942-1-1\n",
                sb.toString());
    }
    /*
     *enum Gender{MALE, FEMALE};
     * struct Date {
     *     unsigned short year;
     *     unsigned byte month;
     *     unsigned byte day;
     * };
     */

    static enum Gender {
        MALE(0),
        FEMALE(1);
        final int code;
        static final Gender[] GENDERS;

        static {
            GENDERS = Stream.of(values()).sorted(Comparator.comparing(Gender::code)).toArray(Gender[]::new);
        }

        public int code() {
            return code;
        }

        Gender(int code) {
            this.code = code;
        }
    }

    static class Date extends Struct<Date> implements BytesMarshallable {
        static final int YEAR = 0;          // short year_
        static final int MONTH = YEAR + 2;  // byte  month_
        static final int DAY = MONTH + 1;   // byte  day_
        static final int SIZE = DAY + 1;

        protected Date construct(long address) {
            return address == 0 ? new Date() : new Date(address);
        }

        // standard new - allocate private memory
        public Date() {
            super(SIZE);
        }

        // placement new - memory is provided and/or managed externally
        public Date(final long address) {
            super(SIZE, address);
        }

        // construct with data
        public Date(short year, byte month, byte day) {
            super(SIZE);
            year(year);
            month(month);
            day(day);
        }

        public short year() {
            return MEMORY.readShort(address + YEAR);
        }

        public Date year(short year) {
            MEMORY.writeShort(address + YEAR, year);
            return this;
        }

        public byte month() {
            return MEMORY.readByte(address + MONTH);
        }

        public Date month(byte month) {
            MEMORY.writeByte(address + MONTH, month);
            return this;
        }

        public byte day() {
            return MEMORY.readByte(address + DAY);
        }

        public Date day(byte day) {
            MEMORY.writeByte(address + DAY, day);
            return this;
        }

        @Override
        public void readMarshallable(BytesIn<?> bytes) throws IORuntimeException, BufferUnderflowException, IllegalStateException {
            // read from bytes to address. advance read position in bytes
            bytes.unsafeRead(address, SIZE);
        }

        @Override
        public void writeMarshallable(BytesOut<?> bytes) throws IllegalStateException, BufferOverflowException, BufferUnderflowException, ArithmeticException {
            // write from address to bytes. advance write position in bytes
            bytes.unsafeWrite(address, SIZE);
        }

        @Override
        public String toString() {
            return year() + "-" + month() + "-" + day();
        }
    }

    /* struct Student {
     *     int locked;
     *     enum Gender gender;
     *     char        name[64];
     *     struct Date birth;
     *     float       grades[10];
     *     Student*    next;
     */

    static class Student extends Struct<Student> implements BytesMarshallable {
        static final int LOCK = 0;
        static final int GENDER = LOCK + 4;
        static final int NAME = GENDER + 4;
        static final int NAME_SIZE = 64;
        static final int BIRTH = NAME + NAME_SIZE;
        static final int GRADES = BIRTH + Date.SIZE;
        static final int NUM_GRADES = 10;
        static final int NEXT = GRADES + NUM_GRADES * Float.BYTES;
        static final int SIZE = NEXT + (Jvm.is64bit() ? 8 : 4);

        Bytes<?> name;
        String nameStr = null;
        Date birth;   // Date instance owned by the this Student
        Pointer<Student> next = new Pointer<>(this::construct);

        protected Student construct(long address) {
            return address == 0 ? new Student() : new Student(address);
        }

        // standard new - we own the memory
        public Student() {
            super(SIZE);
        }

        // placement new - memory is provided to us
        public Student(final long address) {
            super(SIZE, address);
        }

        @Override
        protected void initialise(final long address) {
            // base class does the boilerplating
            super.initialise(address);

            // bespoke controls needed for birth, name, nameStr
            if(birth == null)
                birth = new Date(address + BIRTH);
            else
                birth.initialise(address + BIRTH);

            if(name == null)
                name = new NativeBytes<>(new PointerBytesStore(), NAME_SIZE);

            ((PointerBytesStore) name.bytesStore()).set(address + NAME, NAME_SIZE);
            nameStr = null;
        }

        public void lock() {
            Locks.lock(address);
        }

        public void unlock() {
            Locks.unlock(address);
        }

        public Gender gender() {
            return Gender.GENDERS[MEMORY.readInt(address + GENDER)];
        }

        public Student gender(Gender gender) {
            MEMORY.writeInt(address + GENDER, gender.code);
            return this;
        }

        // return a shared ptr to the this birth member
        public Date birth() {
            return birth;
        }

        public Student birth(int year, int month, int day) {
            birth.year((short) year).month((byte) month).day((byte) day);
            return this;
        }

        // replace birth (self) with copy of the given date
        public Student birth(final Date date) {
            birth.copy(date);
            return this;
        }

        public String name() {
            // use a String pool
            return nameStr != null
                    ? nameStr
                    : (nameStr =
                    name.readPositionRemaining(0, NAME_SIZE)
                            .parse8bit(StopCharTesters.NON_NUL));
        }

        public StringBuilder nameUsing(StringBuilder sb) {
            nameStr = null;
            // use a String pool
            name.clear().parse8bit(sb, StopCharTesters.NON_NUL);
            return sb;
        }

        public Student name(CharSequence cs) {
            name.clear().append8bit(cs);
            if (name.writeRemaining() > 0)
                name.append('\0');
            return this;
        }

        public float grade(int n) {
            assert 0 <= n && n < NUM_GRADES;
            return MEMORY.readFloat(address + GRADES + Float.BYTES * n);
        }

        public Student grade(int n, float f) {
            assert 0 <= n && n < NUM_GRADES;
            MEMORY.writeFloat(address + GRADES + Float.BYTES * n, f);
            return this;
        }

        public Student next() {
            long address = Jvm.is64bit() ? MEMORY.readLong(this.address + NEXT) : MEMORY.readInt(this.address + NEXT);
            next.reset(address);
            return next.get();
        }

        public Student next(Student s) {
            long address = s == null ? 0 : s.address;

            if (Jvm.is64bit())
                MEMORY.writeLong(this.address + NEXT, address);
            else
                MEMORY.writeInt(this.address + NEXT, (int) address);

            return this;
        }

        @Override
        public void readMarshallable(BytesIn<?> bytes) throws IORuntimeException, BufferUnderflowException, IllegalStateException {
            bytes.unsafeRead(address, SIZE);
        }

        @Override
        public void writeMarshallable(BytesOut<?> bytes) throws IllegalStateException, BufferOverflowException, BufferUnderflowException, ArithmeticException {
            bytes.unsafeWrite(address, SIZE);
        }

        public int size() {
            return SIZE;
        }
    }

    enum Locks {
        ; // none

        public static void lock(long address) {
            int threadId = Affinity.getThreadId();
            if (MEMORY.compareAndSwapInt(address, 0, threadId))
                return;
            for (long start = System.currentTimeMillis(); start + 10_000 > System.currentTimeMillis(); ) {
                for (int i = 0; i < 1000; i++)
                    if (MEMORY.compareAndSwapInt(address, 0, threadId))
                        return;
            }
            throw new IllegalStateException("Failed to obtain a lock from process " + MEMORY.readVolatileInt(address));
        }

        public static void unlock(long address) {
            int threadId = Affinity.getThreadId();
            if (MEMORY.compareAndSwapInt(address, threadId, 0))
                return;
            throw new IllegalStateException("Can't release lock held by " + MEMORY.readVolatileInt(address));
        }
    }

    @Test
    public void testCopyingVsSharing() {
        Date d1 = new Date((short)1970, (byte)1, (byte)1);
        Date d2 = d1.copy();
        Date d3 = d1.share();

        assertEquals("1970-1-1", d1.toString());

        d2.month((byte)2); // d2 only
        d3.month((byte)3); // d1 and d3

        assertEquals("1970-3-1", d1.toString());
        assertEquals("1970-3-1", d3.toString());

        assertEquals("1970-2-1", d2.toString());

        // point d3 to d2 (from d1)
        d3.share(d2);
        assertEquals("1970-2-1", d3.toString());

        // change d2 (and so also d3)
        d2.month((byte)4);
        assertEquals("1970-4-1", d2.toString());
        assertEquals("1970-4-1", d3.toString());

        // copy d2 into d3
        d3.copy(d2);

        // change d2; d3 stays the same
        d2.month((byte)5);
        assertEquals("1970-5-1", d2.toString());
        assertEquals("1970-4-1", d3.toString());

    }
}
