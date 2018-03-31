package message;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousFileChannel;
import java.nio.channels.CompletionHandler;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import initiator.Peer;
import sateInfo.BackupFile;
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
	                		System.out.println("Recebeu PUTCHUNK para chunk " + parser.chunkNo);
	                		saveChunkInfo(parser);
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
    	Random r = new Random();
    	ChunkBackup subprotocol = new ChunkBackup(parser.version, Peer.id, parser.senderID, parser.fileID, parser.chunkNo, parser.body, parser.replicationDeg);
    	SingletonThreadPoolExecutor.getInstance().getThreadPoolExecutor().schedule(subprotocol, (long) r.nextInt(400), TimeUnit.MILLISECONDS);
    }
    
    public void saveChunkInfo(Parser parser) {
    	BackupFile file = LocalState.getInstance().getBackupFiles().get(parser.fileID);
		if(file == null) {
			Chunk chunk = new Chunk(parser.chunkNo, parser.replicationDeg, (long) parser.body.length, myID);
			LocalState.getInstance().saveChunk(parser.fileID, null, myID, parser.replicationDeg, chunk);
			LocalState.getInstance().decreaseReplicationDegree(parser.fileID, parser.chunkNo, myID, myID);
		} else {
			Chunk chunk = file.getChunks().get(parser.chunkNo);
			if(chunk == null) {
				chunk = new Chunk(parser.chunkNo, parser.replicationDeg, (long) parser.body.length, myID);
				LocalState.getInstance().saveChunk(parser.fileID, null, myID, parser.replicationDeg, chunk);
				LocalState.getInstance().decreaseReplicationDegree(parser.fileID, parser.chunkNo, myID, myID);
			} else {
				chunk.setReplicationDeg(parser.replicationDeg);
			}
			chunk.setReclaimMode(Chunk.State.OFF);
		}
    }
    
    
    
}
