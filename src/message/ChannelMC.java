package message;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.UnknownHostException;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import initiator.Peer;
import sateInfo.Chunk;
import sateInfo.LocalState;
import subprotocols.ChunkRestore;
import subprotocols.Deletion;

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
							LocalState.getInstance().updateReplicationInfo(parser.senderID, parser.fileID, parser.chunkNo);
						} else if(parser.messageType.equals("DELETE")) {
							System.err.println("ESTOU A APAGAR!!!");
							Deletion subprotocol = new Deletion(parser);
							SingletonThreadPoolExecutor.getInstance().getThreadPoolExecutor().execute(subprotocol);
//							System.out.println("Apagou ficheiro " + parser.fileName);
//							System.out.println("Agora tem os seguintes ficheiros guardados:");
//							for (Entry<String, BackupFile> entry : LocalState.getInstance().getBackupFiles().entrySet()) {
//							    System.out.println(entry.getKey());
//							}
						} else if(parser.messageType.equals("GETCHUNK")) {
							if(LocalState.getInstance().getBackupFiles().get(parser.fileID) != null) {
								Chunk chunk = LocalState.getInstance().getBackupFiles().get(parser.fileID).getChunks().get(parser.chunkNo);
								if(chunk != null) {
									chunk.setRestoreMode(Chunk.State.ON);
									ChunkRestore subprotocol = new ChunkRestore(parser);
									Random r = new Random();
					        		SingletonThreadPoolExecutor.getInstance().getThreadPoolExecutor().schedule(subprotocol, (long) r.nextInt(400), TimeUnit.MILLISECONDS);
									//LocalState.getInstance().returnToFalse(parser.fileID, parser.chunkNo);
								}
							}
						} else if(parser.messageType.equals("DELETED")) {
							if(LocalState.getInstance().getBackupFiles().get(parser.fileID) != null) {
								LocalState.getInstance().decreaseReplicationDegree(parser.fileID, parser.senderID);
								if(LocalState.getInstance().isReplicationDegreeZero(parser.fileID)) {
									LocalState.getInstance().getBackupFiles().remove(parser.fileID);
								}
								//System.err.println("decreaseReplicationDegree");
							}
							//System.err.println("TO DO");
						} else if(parser.messageType.equals("CHECKDELETE")) {
							if(LocalState.getInstance().getBackupFiles().get(parser.fileID) != null) {
								if(LocalState.getInstance().getBackupFiles().get(parser.fileID).getWasDeleted()) {
									if(Peer.sendDeleteMessage(1.2, Peer.id, parser.fileID) == -1) {
										System.err.println("Error: Could not send DELETE message.");
										return;
									}
								}
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

}
