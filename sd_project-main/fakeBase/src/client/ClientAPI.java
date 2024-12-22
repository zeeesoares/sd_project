package client;

import protocol.TaggedConnection;
import protocol.TaggedFrame;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

//Atualizei o resto

public class ClientAPI {

    private TaggedConnection connection;

    public enum RequestType {PUT, GET, MULTIPUT, MULTIGET, GETWHEN}

    public ClientAPI(String host, int port) throws Exception {
        connection = new TaggedConnection(new java.net.Socket(host, port));
    }

    public void put(String key, byte[] value) throws Exception {
        String payload = key + ":" + new String(value);
        connection.send(new TaggedFrame(2, payload.getBytes()));

        TaggedFrame response = connection.receive();
        System.out.println("PUT Response: " + new String(response.getData()));
    }

    public byte[] get(String key) throws Exception {
        connection.send(new TaggedFrame(3, key.getBytes()));
        TaggedFrame response = connection.receive();
        return response.getData();
    }

    public void multiPut(Map<String, byte[]> pairs) throws Exception {
        StringBuilder payload = new StringBuilder();
        for (Map.Entry<String, byte[]> entry : pairs.entrySet()) {
            payload.append(entry.getKey()).append(":").append(new String(entry.getValue())).append(";");
        }
        connection.send(new TaggedFrame(5, payload.toString().getBytes()));

        TaggedFrame response = connection.receive();
        System.out.println("MULTIPUT Response: " + new String(response.getData()));
    }

    public Map<String, byte[]> multiGet(Set<String> keys) throws Exception {
        StringBuilder payload = new StringBuilder();
        for (String key : keys) {
            payload.append(key).append(";");
        }
        connection.send(new TaggedFrame(4, payload.toString().getBytes()));
        TaggedFrame response = connection.receive();

        Map<String, byte[]> results = new HashMap<>();
        String[] pairs = new String(response.getData()).split(";");
        for (String pair : pairs) {
            if (!pair.isEmpty()) {
                String[] keyValue = pair.split(":");
                if (keyValue.length == 2) {
                    results.put(keyValue[0], keyValue[1].getBytes());
                }
            }
        }
        return results;
    }

    public byte[] getWhen(String key, String keyCond, byte[] valueCond) throws Exception {
        String payload = key + ":" + keyCond + ":" + new String(valueCond);
        connection.send(new TaggedFrame(6, payload.getBytes()));
        TaggedFrame response = connection.receive();
        return response.getData();
    }

    public void close() throws Exception {
        connection.close();
    }
}

