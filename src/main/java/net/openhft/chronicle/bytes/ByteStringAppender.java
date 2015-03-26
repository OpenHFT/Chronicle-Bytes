package net.openhft.chronicle.bytes;

public interface ByteStringAppender<B extends ByteStringAppender<B, A, AT>,
        A extends WriteAccess<AT>, AT> extends StreamingDataOutput<B, A, AT> {
    default B append(char ch) {
        BytesUtil.appendUTF(this, ch);
        return (B) this;
    }

    default B append(CharSequence cs) {
        return append(cs, 0, cs.length());
    }

    default B append(long value) {
        BytesUtil.append(this, value);
        return (B) this;
    }

    default B append(float f) {
        BytesUtil.append(this, f);
        return (B) this;
    }

    default B append(double d) {
        BytesUtil.append(this, d);
        return (B) this;
    }

    default B append(CharSequence cs, int start, int end) {
        BytesUtil.appendUTF(this, cs, start, end - start);
        return (B) this;
    }

    default B append(long value, int digits) {
        BytesUtil.append((RandomDataOutput) this, position(), value, digits);
        this.skip(digits);
        return (B) this;
    }

}
