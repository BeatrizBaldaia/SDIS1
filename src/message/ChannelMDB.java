package message;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import initiator.Peer;
import sateInfo.Chunk;
import sateInfo.LocalState;
import subprotocols.ChunkBackup;

public class ChannelMDB {
	private static ChannelMDB instance = null;
	
	private MulticastSocket socket;
    private InetAddress address;
    private int port;
    private int myID;

    public ChannelMDB() {}

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
    public void createMulticastSocket(String addressStr, String portStr, int myID) {
    	this.myID = myID;
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
                    Parser parser = new Parser(msg, packet.getLength());
                	if(parser.parseHeader() != 0) {
                		System.out.println("Error parsing the message");
                	}
                	//System.err.println("Gets to listem!");
                	if(parser.senderID != myID) { //
	                	if(parser.messageType.equals("PUTCHUNK")) {
	                		Chunk chunk = LocalState.getInstance().getBackupFiles().get(parser.fileID).getChunks().get(parser.chunkNo);
	                		chunk.setReclaimMode(Chunk.State.OFF);
	                		handlePutChunkMsg(parser);
	                	}
                	}
                } catch (IOException e) {
                    e.printStackTrace();
                    return;
                }
            }
        }).start();
    	
    }
    
    /**
     * Process the PUTCHUNK message
     * @param parser
     */
    public void handlePutChunkMsg(Parser parser) {
    	try {
			if((parser.body.length + LocalState.getInstance().getUsedStorage()) <= LocalState.getInstance().getStorageCapacity()) {
				storeChunk(parser);
				ChunkBackup subprotocol = new ChunkBackup(parser.version, Peer.id, parser.fileID, parser.chunkNo);
        		
        		Random r = new Random();
        		SingletonThreadPoolExecutor.getInstance().getThreadPoolExecutor().schedule(subprotocol, (long) r.nextInt(400), TimeUnit.MILLISECONDS);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
    }
    
    /**
     * Stores the chunk if it doesn't exists
     * @param parser
     * @throws IOException
     */
    public void storeChunk(Parser parser) throws IOException {
    	byte[] body = Arrays.copyOf(parser.body, parser.body.length);
    	Chunk chunk = new Chunk(parser.chunkNo, parser.replicationDeg, (long) body.length, Peer.id);
		LocalState.getInstance().saveChunk(parser.fileID, null, parser.senderID, parser.replicationDeg, chunk);

		Path filePath = Peer.getP().resolve(parser.fileID + "_" + parser.chunkNo);
		if(!Files.exists(filePath)) { //NOTE: O CHUNk nao Existe
			System.out.println("Criar ficheiro: "+filePath);
			Files.createFile(filePath);
			Files.write(filePath, body);
		}
    }
    
}
