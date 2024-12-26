// ClientAPI.java
package client;

import protocol.Demultiplexer;
import protocol.TaggedConnection;
import java.io.IOException;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class ClientAPI implements AutoCloseable {

    private final Demultiplexer demux;

    public ClientAPI(String host, int port) throws IOException {
        Socket socket = new Socket(host, port);
        this.demux = new Demultiplexer(new TaggedConnection(socket));
        this.demux.start();
    }

    public void close() throws IOException {
        this.demux.close();
    }

    public String register(String username, String password) throws IOException, InterruptedException {
        String registrationData = username + ":" + password;
        demux.send(1, registrationData.getBytes());
        byte[] response = demux.receive(1);
        return new String(response);
    }

    public String put(String key, String value) throws IOException, InterruptedException {
        String putData = key + ":" + value;
        demux.send(2, putData.getBytes());
        byte[] response = demux.receive(2);
        return new String(response);
    }

    public String get(String key) throws IOException, InterruptedException {
        demux.send(3, key.getBytes());
        byte[] response = demux.receive(3);
        return new String(response);
    }

    public String multiPut(Map<String, String> keyValuePairs) throws IOException, InterruptedException {
        StringBuilder requestBuilder = new StringBuilder();
    
        for (Map.Entry<String, String> entry : keyValuePairs.entrySet()) {
            requestBuilder.append(entry.getKey())
                    .append(":")
                    .append(entry.getValue())
                    .append(";");
        }
    
        demux.send(4, requestBuilder.toString().getBytes());
    
        byte[] response = demux.receive(4);
        return new String(response);
    }

    public Map<String, String> multiGet(Set<String> keys) throws IOException, InterruptedException {
        StringBuilder requestBuilder = new StringBuilder();
    
        for (String key : keys) {
            requestBuilder.append(key).append(";");
        }
    
        demux.send(5, requestBuilder.toString().getBytes());
    
        byte[] response = demux.receive(5);
    
        Map<String, String> result = new HashMap<>();
        String[] keyValuePairs = new String(response).split(";");
        for (String pair : keyValuePairs) {
            String[] keyValue = pair.split(":");
            if (keyValue.length == 2) {
                result.put(keyValue[0], keyValue[1]);
            }
        }
    
        return result;
    }

    public byte[] getWhen(String key, String keyCond, byte[] valueCond) throws IOException, InterruptedException {
        String request = key + ":" + keyCond + ":" + new String(valueCond);

        demux.send(6, request.getBytes());

        byte[] response = demux.receive(6);

        return response;
    }
    
    

}
