package message;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.UnknownHostException;

import subprotocols.ChunkBackup;

public class ChannelMDB {
	private static ChannelMDB instance = null;
	
	private MulticastSocket socket;
    private InetAddress address;
    private int port;

    public ChannelMDB() {
    }


    public static ChannelMDB getInstance() {
    	if(instance == null) {
    		instance = new ChannelMDB();
    	}
    	return instance;
    }
    
    /**
     * Creates the multicast data channel socket
     * @param addressStr
     * @param portStr
     */
    public void createMulticastSocket(String addressStr, String portStr) {
        try {
            address = InetAddress.getByName(addressStr);
        } catch (UnknownHostException e) {
            System.err.println("Error parsing address: " + addressStr);
            e.printStackTrace();
        }
        
        port = Integer.parseInt(portStr);

        MulticastSocket socket = null;
        try {
            socket = new MulticastSocket(port);
            socket.setTimeToLive(1);
            socket.joinGroup(address);
        } catch (IOException e) {
            e.printStackTrace();
        }

        this.socket = socket;
    }
    
    /**
     * 
     * @return the address of the multicast data channel socket
     */
    public InetAddress getAddress() { return address; }
    
    /**
     * 
     * @return the port of the multicast data channel socket
     */
    public int getPort() { return port; }
    

    /**
     * Sends message.
     *
     * @param message to be sent.
     */
    public void sendMessage(byte[] message) {
        DatagramPacket packet = new DatagramPacket(message, message.length, address, port);

        try {
            socket.send(packet);
        } catch (IOException ignored) {
        }
    }

    /**
     * Listens to the socket for messages.
     * Block until receives the message
     */
    public void listen() {
       new Thread(() -> {
            while (true) {
                byte[] buffer = new byte[200 + 64 * 1000];//header + body
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);

                try {
                    socket.receive(packet);
                    byte[] msg = packet.getData();
                    Parser parser = new Parser(msg, msg.length);
                	if(parser.parseHeader() != 0) {
                		System.out.println("Error parsing the message");
                	}
                	if(parser.messageType.equals("PUTCHUNK")) {
                		ChunkBackup subprotocol = new ChunkBackup(parser);
                		SingletonThreadPoolExecutor.getInstance().getThreadPoolExecutor().execute(subprotocol);
                	}
                } catch (IOException e) {
                    e.printStackTrace();
                    return;
                }
            }
        }).start();
    	
    }
}
