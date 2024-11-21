import java.io.*;
import java.util.Arrays;

public class ByteArrayWrapper {

    private byte[] value;

    public ByteArrayWrapper(byte[] value) {
        this.value = value;
    }

    public byte[] getValue() {
        return value;
    }

    public int length() {
        return value.length;
    }

    public void serialize(DataOutputStream out) throws IOException {
        out.writeInt(this.length());
        out.write(this.getValue());
    }

    public boolean equals(ByteArrayWrapper other) {
        return Arrays.equals(this.value, other.value);
    }

    public byte[] deepCopy() {
        return Arrays.copyOf(value, value.length);
    }

    @Override
    public String toString() {
        return "ByteArrayWrapper{" + "value=" + Arrays.toString(value) + '}';
    }
}
