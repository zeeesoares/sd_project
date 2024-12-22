package protocol;

import java.util.HashMap;
import java.util.Map;

//Atualizado o Request com o necessário, pode sofrer alterações

public abstract class Request {
    protected int tag;

    public Request(int tag) {
        this.tag = tag;
    }

    public int getTag() {
        return tag;
    }

    public abstract byte[] toBytes();

    public static Request fromBytes(int tag, byte[] data) {
        switch (tag) {
            case 2: return new PutRequest(data);
            case 3: return new GetRequest(data);
            case 4: return new MultiGetRequest(data);
            case 5: return new MultiPutRequest(data);
            case 6: return new GetWhenRequest(data);
            default: throw new IllegalArgumentException("Invalid tag: " + tag);
        }
    }
}

class PutRequest extends Request {
    private final String key;
    private final byte[] value;

    public PutRequest(byte[] data) {
        super(2);
        String[] parts = new String(data).split(":");
        this.key = parts[0];
        this.value = parts[1].getBytes();
    }

    public String getKey() {
        return key;
    }

    public byte[] getValue() {
        return value;
    }

    @Override
    public byte[] toBytes() {
        return (key + ":" + new String(value)).getBytes();
    }
}

class GetRequest extends Request {
    private final String key;

    public GetRequest(byte[] data) {
        super(3);
        this.key = new String(data);
    }

    public String getKey() {
        return key;
    }

    @Override
    public byte[] toBytes() {
        return key.getBytes();
    }
}

class MultiGetRequest extends Request {
    private final String[] keys;

    public MultiGetRequest(byte[] data) {
        super(4);
        this.keys = new String(data).split(";");
    }

    public String[] getKeys() {
        return keys;
    }

    @Override
    public byte[] toBytes() {
        return String.join(";", keys).getBytes();
    }
}

class MultiPutRequest extends Request {
    private final Map<String, byte[]> keyValuePairs = new HashMap<>();

    public MultiPutRequest(byte[] data) {
        super(5);
        String[] pairs = new String(data).split(";");
        for (String pair : pairs) {
            String[] keyValue = pair.split(":");
            keyValuePairs.put(keyValue[0], keyValue[1].getBytes());
        }
    }

    public Map<String, byte[]> getKeyValuePairs() {
        return keyValuePairs;
    }

    @Override
    public byte[] toBytes() {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, byte[]> entry : keyValuePairs.entrySet()) {
            sb.append(entry.getKey()).append(":").append(new String(entry.getValue())).append(";");
        }
        return sb.toString().getBytes();
    }
}

class GetWhenRequest extends Request {
    private final String key;
    private final String keyCond;
    private final byte[] valueCond;

    public GetWhenRequest(byte[] data) {
        super(6);
        String[] parts = new String(data).split(":");
        this.key = parts[0];
        this.keyCond = parts[1];
        this.valueCond = parts[2].getBytes();
    }

    public String getKey() {
        return key;
    }

    public String getKeyCond() {
        return keyCond;
    }

    public byte[] getValueCond() {
        return valueCond;
    }

    @Override
    public byte[] toBytes() {
        return (key + ":" + keyCond + ":" + new String(valueCond)).getBytes();
    }
}