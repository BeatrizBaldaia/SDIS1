package message;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.UnknownHostException;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import initiator.Peer;
import sateInfo.BackupFile;
import sateInfo.Chunk;
import sateInfo.LocalState;
import subprotocols.CheckDeletedFile;
import subprotocols.ChunkRestore;
import subprotocols.Deletion;
import subprotocols.IterativeDelete;
import subprotocols.Reclaiming;
import subprotocols.UpdateRepDeg;

public class ChannelMC {
	private static ChannelMC instance = null;

	private MulticastSocket socket;
	private InetAddress address;
	private int port;
	private int myID;

	public ChannelMC() {}

	public static ChannelMC getInstance() {
		if(instance == null) {
			instance = new ChannelMC();
		}
		return instance;
	}
	/**
	 * @param addressStr
	 * @param portStr
	 * @param myID
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
					if(parser.senderID != myID) {
						//Receber mensagens STORED, GETCHUNK, DELETE e REMOVED
						if(parser.messageType.equals("STORED")) {
							UpdateRepDeg subprotocol = new UpdateRepDeg(parser.senderID, parser.fileID, parser.chunkNo);
							SingletonThreadPoolExecutor.getInstance().getThreadPoolExecutor().submit(subprotocol);
						} else if(parser.messageType.equals("DELETE")) {
							System.out.println("Recebeu do peer " + parser.senderID + " msg DELETE para o file " + parser.fileID);
							Deletion subprotocol = new Deletion(parser);
							SingletonThreadPoolExecutor.getInstance().getThreadPoolExecutor().submit(subprotocol);
						} else if(parser.messageType.equals("GETCHUNK")) {
							System.out.println("Recebeu do peer " + parser.senderID + " msg GETCHUNK para o file " + parser.fileID + ", chunk " + parser.chunkNo);
							if(isStoringChunk(parser)) {
								ChunkRestore subprotocol = new ChunkRestore(parser);
								Random r = new Random();
								SingletonThreadPoolExecutor.getInstance().getThreadPoolExecutor().schedule(subprotocol, (long) r.nextInt(400), TimeUnit.MILLISECONDS);
							}
						} else if(parser.messageType.equals("DELETED")) {
							System.out.println("Recebeu do peer " + parser.senderID + " msg DELETED para o file " + parser.fileID);
							IterativeDelete subprotocol = new IterativeDelete(parser.senderID, parser.fileID);
							SingletonThreadPoolExecutor.getInstance().getThreadPoolExecutor().submit(subprotocol);
						} else if(parser.messageType.equals("CHECKDELETE")) {
							System.out.println("Recebeu do peer " + parser.senderID + " msg CHECKDELETE para o file " + parser.fileID);
							CheckDeletedFile subprotocol = new CheckDeletedFile(Peer.id, 1.2, parser.fileID);
							SingletonThreadPoolExecutor.getInstance().getThreadPoolExecutor().submit(subprotocol);
						} else if(parser.messageType.equals("REMOVED")) {
							if(lowReplicationDegree(parser)) {
								Reclaiming subprotocol = new Reclaiming(parser);
				        		Random r = new Random();
				        		SingletonThreadPoolExecutor.getInstance().getThreadPoolExecutor().schedule(subprotocol, (long) r.nextInt(400), TimeUnit.MILLISECONDS);
							}
						} else {
							System.err.println("Error: Does not recognize type of message.");
						}
					}
				} catch (IOException e) {
					e.printStackTrace();
					return;
				}
			}
		}).start();

	}
	
	public void saveChunkInfo(Parser parser) {
		BackupFile file = LocalState.getInstance().getBackupFiles().get(parser.fileID);
		if(file == null) {
			Chunk chunk = new Chunk(parser.chunkNo, parser.replicationDeg, (long) parser.body.length, myID);
			LocalState.getInstance().saveChunk(parser.fileID, null, parser.senderID, parser.replicationDeg, chunk);
			LocalState.getInstance().decreaseReplicationDegree(parser.fileID, parser.chunkNo, parser.senderID, myID);
		} else {
			Chunk chunk = file.getChunks().get(parser.chunkNo);
			if(chunk == null) {
				chunk = new Chunk(parser.chunkNo, parser.replicationDeg, (long) parser.body.length, myID);
				LocalState.getInstance().saveChunk(parser.fileID, null, parser.senderID, parser.replicationDeg, chunk);
				LocalState.getInstance().decreaseReplicationDegree(parser.fileID, parser.chunkNo, parser.senderID, myID);
			}
		}
	}

	public boolean isStoringChunk(Parser parser) {
		if(LocalState.getInstance().getBackupFiles().get(parser.fileID) != null) {
			Chunk chunk = LocalState.getInstance().getBackupFiles().get(parser.fileID).getChunks().get(parser.chunkNo);
			if(chunk != null && chunk.isStoringChunk()) {
				chunk.setRestoreMode(Chunk.State.ON);
				return true;
			}
		}
		return false;
	}
	
	public boolean lowReplicationDegree(Parser parser) {
		try {
			LocalState.getInstance().decreaseReplicationDegree(parser.fileID, parser.chunkNo, parser.senderID, Peer.id);
			} catch(NullPointerException e) {
				System.out.println("Problem decreasing the replication degree");
				return false;
			}
			if(!LocalState.getInstance().getBackupFiles().get(parser.fileID).desireReplicationDeg(parser.chunkNo)) {
				Chunk chunk = LocalState.getInstance().getBackupFiles().get(parser.fileID).getChunks().get(parser.chunkNo);
				chunk.setReclaimMode(Chunk.State.ON);
				return true;
			}
			return false;
	}

}
