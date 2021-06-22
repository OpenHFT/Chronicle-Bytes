package net.openhft.chronicle.bytes;

import net.openhft.affinity.Affinity;
import net.openhft.chronicle.core.Jvm;
import net.openhft.chronicle.core.io.IORuntimeException;
import org.junit.Test;

import java.nio.BufferOverflowException;
import java.nio.BufferUnderflowException;
import java.util.Comparator;
import java.util.stream.Stream;

import static net.openhft.chronicle.core.UnsafeMemory.MEMORY;
import static org.junit.Assert.assertEquals;

public class StructTest {

    @Test
    public void createStudents() {
        Bytes stud1 = Bytes.allocateDirect(Student.SIZE);
        Bytes stud2 = Bytes.allocateDirect(Student.SIZE);
        Bytes stud3 = Bytes.allocateDirect(Student.SIZE);
        Student s3 = new Student()
                .address(stud3.addressForWrite(0))
                .gender(Gender.FEMALE)
                .name("Wonder Woman")
                .birth(1942, 1, 1)
                .grade(0, 0.95f);
        Student s2 = new Student()
                .address(stud2.addressForWrite(0))
                .gender(Gender.MALE)
                .name("Superman")
                .birth(1938, 4, 18)
                .grade(0, 0.96f)
                .next(s3.address);
        Student s1 = new Student()
                .address(stud1.addressForWrite(0))
                .gender(Gender.MALE)
                .name("The Phantom")
                .birth(1936, 2, 17)
                .grade(0, 0.97f)
                .next(s2.address);

        for (Bytes bytes : new Bytes[]{stud1, stud2, stud3}) {
            bytes.readLimit(Student.SIZE);
            System.out.println(bytes.toHexString());
        }

        StringBuilder sb = new StringBuilder();
        Student s = new Student();
        for (long address = s1.address; address != 0; address = s.next()) {
            s.address(address);
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

    static class Date implements BytesMarshallable {
        static final int YEAR = 0;
        static final int MONTH = YEAR + 2;
        static final int DAY = MONTH + 1;
        static final int SIZE = DAY + 1;
        Bytes<?> temp = Bytes.allocateDirect(SIZE);
        long address;

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
        public void readMarshallable(BytesIn bytes) throws IORuntimeException, BufferUnderflowException, IllegalStateException {
            address = temp.addressForWrite(0);
            temp.clear();
            bytes.read(temp, SIZE);
        }

        @Override
        public void writeMarshallable(BytesOut bytes) throws IllegalStateException, BufferOverflowException, BufferUnderflowException, ArithmeticException {
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

    static class Student implements BytesMarshallable {
        static final int LOCK = 0;
        static final int GENDER = LOCK + 4;
        static final int NAME = GENDER + 4;
        static final int NAME_SIZE = 64;
        static final int BIRTH = NAME + NAME_SIZE;
        static final int GRADES = BIRTH + Date.SIZE;
        static final int NUM_GRADES = 10;
        static final int NEXT = GRADES + NUM_GRADES * Float.BYTES;
        static final int SIZE = NEXT + (Jvm.is64bit() ? 8 : 4);

        Bytes<?> temp = Bytes.allocateDirect(SIZE);
        Bytes<?> name = new NativeBytes<>(new PointerBytesStore(), NAME_SIZE);
        String nameStr = null;
        Date birth = new Date();
        long address;

        public long address() {
            return address;
        }

        public Student address(long address) {
            assert address != 0;
            this.address = address;
            birth.address = address + BIRTH;
            ((PointerBytesStore) name.bytesStore()).set(address + NAME, NAME_SIZE);
            nameStr = null;
            return this;
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

        public Date birthUsing(Date date) {
            date.address = address + BIRTH;
            return date;
        }

        public Student birth(int year, int month, int day) {
            birth.year((short) year).month((byte) month).day((byte) day);
            return this;
        }

        public Date birth() {
            return birthUsing(birth);
        }

        public Student birth(Date date) {
            MEMORY.copyMemory(address + BIRTH, date.address, Date.SIZE);
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

        public long next() {
            return Jvm.is64bit() ? MEMORY.readLong(address + NEXT) : MEMORY.readInt(address + NEXT);
        }

        public Student next(long address) {
            if (Jvm.is64bit())
                MEMORY.writeLong(this.address + NEXT, address);
            else
                MEMORY.writeInt(this.address + NEXT, (int) address);
            return this;
        }

        @Override
        public void readMarshallable(BytesIn bytes) throws IORuntimeException, BufferUnderflowException, IllegalStateException {
            address = temp.addressForWrite(0);
            temp.clear();
            bytes.read(temp, NEXT);
        }

        @Override
        public void writeMarshallable(BytesOut bytes) throws IllegalStateException, BufferOverflowException, BufferUnderflowException, ArithmeticException {
            bytes.unsafeWrite(address, NEXT);
        }

        public int size() {
            return SIZE;
        }
    }

    enum Locks {
        ;

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
}
