package protocol;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class TaggedFrame {
    private final int tag;
    private final byte[] data;

    public TaggedFrame(int tag, byte[] data) {
        this.tag = tag;
        this.data = data;
    }

    public int getTag() {
        return tag;
    }

    public byte[] getData() {
        return data;
    }
    
    public void serialize(DataOutputStream out) throws IOException {
        out.writeInt(tag);
        out.writeInt(data.length);
        out.write(data);
    }

    public static TaggedFrame deserialize(DataInputStream in) throws IOException {
        int tag = in.readInt();
        int length = in.readInt();
        byte[] data = new byte[length];
        in.readFully(data);
        return new TaggedFrame(tag, data);
        
    }


    @Override
    public String toString() {
        return "Tag: " + this.tag + " | Data: " + this.data.toString();
    }
}
