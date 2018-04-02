package initiator;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Set;
import java.util.TreeSet;

import javax.xml.bind.DatatypeConverter;

import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousFileChannel;
import java.nio.channels.CompletionHandler;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;

import message.ChannelMC;
import message.ChannelMDB;
import message.ChannelMDR;
import message.SingletonThreadPoolExecutor;
import sateInfo.BackupFile;
import sateInfo.Chunk;
import sateInfo.Chunk.State;
import sateInfo.LocalState;
import sateInfo.Pair;
import server.InterfaceApp;
import subprotocols.SendPutChunk;

public class Peer implements InterfaceApp {
	private static double protocolVersion;
	public static int id;
	private static String serviceAccessPoint;
	private static Path p;
	
	private static ChannelMC mc;
	private static ChannelMDB mdb;
	private static ChannelMDR mdr;
	
	public static void main(String[] args) throws IOException {
			if (args.length != 9) {
				System.out.println("Usage: java Initiator.Peer <protocol_version> <server_id> <service_access_point> <MC_ip> <MC_port> <MDB_ip> <MDB_port> <MDR_ip> <MDR_port>");
				return;
			}

			protocolVersion = Integer.parseInt(args[0]);
			id = Integer.parseInt(args[1]);
			serviceAccessPoint = args[2];

			String host = serviceAccessPoint.split(":")[0];
			Integer port = Integer.valueOf(serviceAccessPoint.split(":")[1]);
			
			mc = ChannelMC.getInstance();
			mc.createMulticastSocket(args[3], args[4], id);
			mdb = ChannelMDB.getInstance();
			mdb.createMulticastSocket(args[5], args[6], id);
			mdr = ChannelMDR.getInstance();
			mdr.createMulticastSocket(args[7], args[8], id);
			
			setP(Paths.get("peer_"+id));
			if(!Files.exists(getP())) {
				Files.createDirectory(getP());
			} 
			
			mc.listen();
			mdb.listen();
			mdr.listen();
			
			checkWhichChunksAreInMe();
			
			try {
				Peer obj = new Peer();
				InterfaceApp protocol = (InterfaceApp) UnicastRemoteObject.exportObject(obj, 0);

				// Bind the remote object's stub in the registry
				Registry registry = LocateRegistry.getRegistry(host,port);
				registry.rebind("peer_"+id, protocol);

				System.out.println("Server ready");
			} catch (Exception e) {
				System.err.println("Server exception: " + e.toString());
				e.printStackTrace();
			}
		}
	
	private static void checkWhichChunksAreInMe() throws UnsupportedEncodingException {
		Path dir = Peer.getP();
		File directory = dir.toFile();
		File[] files = directory.listFiles();
		Set<String> fileIDs = new TreeSet<String>();
		for(int i = 0; i<files.length; i++) {
			String filename = files[i].getName();
			Path path = Peer.getP().resolve(filename);
			String[] elem = filename.split("_");
			if (elem.length == 1) {
				try {
					Files.delete(path);
				} catch (IOException e) {
					e.printStackTrace();
				}
			} else {
				String fileID = elem[0];
				Integer chunkNo = Integer.valueOf(elem[1]);
				Long size;
				try {
					size = (Long) Files.getAttribute(path, "size");
				} catch (IOException e) {
					e.printStackTrace();
					return;
				}
				Chunk chunk = new Chunk(chunkNo, 1, size, Peer.id);
				LocalState.getInstance().saveChunk(fileID, null, Peer.id, 1, chunk);
				fileIDs.add(fileID);
			}
			
		}
		String[] filesNames = fileIDs.toArray(new String[0]);
		for(int i = 0; i<filesNames.length; i++) {
			sendCheckDeleteMessage(filesNames[i]);
		}
	}

	private static void sendCheckDeleteMessage(String fileID) throws UnsupportedEncodingException {
		String msg = createCheckDeleteMessage(fileID);
		ChannelMC.getInstance().sendMessage(msg.getBytes("ISO-8859-1"));
		System.out.println(msg);
	}

	private static String createCheckDeleteMessage(String fileID) {
		String msg = "CHECKDELETE "+ Peer.protocolVersion + " " + Peer.id + " " + fileID +" \r\n\r\n";
		return msg;
	}

	/**
	 * 
	 * @param version of the protocol
	 * @param senderID who is going to send the message
	 * @param fileID ; file identifier for the backup service
	 * @param chunkNo (chunkNo + fileID = specific chunk in a file)
	 * @param replicationDeg ; replication degree of the chunk
	 * @param body ; file data
	 * @return the PUTCHUNK message to be sent
	 * @throws UnsupportedEncodingException 
	 */
	public static String createPutChunkMessage(double version, int senderID, String fileID, int chunkNo, int replicationDeg, byte [] body) throws UnsupportedEncodingException {
		String bodyStr = new String(body, "ISO-8859-1"); // for ISO-8859-1 encoding
//		System.err.println("bodyStr.lenght:"+bodyStr.length());
//		System.err.println("bodyStr.getBytes.size:"+bodyStr.getBytes("ISO-8859-1").length);
		String msg = "PUTCHUNK "+ version + " " + senderID + " " + fileID+ " " + chunkNo + " " + replicationDeg + " \r\n\r\n" + bodyStr;
		return msg;
	}
	
	/**
	 * 
	 * @param version of the protocol
	 * @param senderID who is going to send the message
	 * @param fileID ; file identifier for the backup service
	 * @return the PUTCHUNK message to be sent
	 * @throws UnsupportedEncodingException 
	 */
	public static String createDeleteMessage(double version, int senderID, String fileID) throws UnsupportedEncodingException {
		String msg = "DELETE "+ version + " " + senderID + " " + fileID + " \r\n\r\n";
		return msg;
	}
	
	/**
	 * 
	 * @param version of the protocol
	 * @param senderID who is going to send the message
	 * @param fileID ; file identifier for the backup service
	 * @param chunkNo ; chunk identifier
	 * @return the REMOVED message to be sent
	 */
	public String createRemovedMessage(double version, int senderID, String fileID, int chunkNo) {
		String msg = "REMOVED "+ version + " " + senderID + " " + fileID + " " + chunkNo + " \r\n\r\n";
		return msg;
	}
	
	/**
	 * sends the PUTCHUNK message to the MDB channel
	 * @param version
	 * @param senderID
	 * @param fileID
	 * @param chunkNo
	 * @param replicationDeg
	 * @param body
	 * @return different of 0 when error 
	 * @throws UnsupportedEncodingException 
	 */
	public static int sendPutChunkMessage(double version, int senderID, String fileID, int chunkNo, int replicationDeg, byte [] body) throws UnsupportedEncodingException {
		
		String msg = null;
		try {
			msg = createPutChunkMessage(version, senderID, fileID, chunkNo, replicationDeg, body) ;
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
			return -1;
		}
		
		ChannelMDB.getInstance().sendMessage(msg.getBytes("ISO-8859-1"));
		//System.out.println("SENT --> "+msg);
		System.out.println("SENT --> "+ msg.split("\r\n")[0]);//PUTCHUNK
		return 0;
	}

/**
	 * sends the DELETE message to the MC channel
	 * @param version
	 * @param senderID
	 * @param fileID
	 * @return
	 * @throws UnsupportedEncodingException 
	 */
	public static int sendDeleteMessage(double version, int senderID, String fileID) throws UnsupportedEncodingException {
			String msg = null;
			try {
				msg = createDeleteMessage(version, senderID, fileID) ;
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
				return -1;
			}
			
			ChannelMC.getInstance().sendMessage(msg.getBytes("ISO-8859-1"));
			System.out.println("SENT --> "+msg);//DELETE
			return 0;
		}
	
	/**
	 * sends the REMOVED message to the MC channel
	 * @param version
	 * @param senderID
	 * @param fileID
	 * @param chunkNo
	 * @return
	 * @throws UnsupportedEncodingException
	 */
	public int sendRemovedMessage(double version, int senderID, String fileID, int chunkNo) throws UnsupportedEncodingException {
		String msg = null;
		msg = createRemovedMessage(version, senderID, fileID, chunkNo) ;
		
		ChannelMC.getInstance().sendMessage(msg.getBytes("ISO-8859-1"));
		System.out.println("SENT --> "+msg);//REMOVED
		return 0;
	}

	/* (non-Javadoc)
	 * @see server.InterfaceApp#backupFile(java.lang.String, java.lang.Integer, java.lang.Boolean)
	 */
	@Override
	public void backupFile(String filename, Integer replicationDegree, Boolean isEnhancement) throws NoSuchAlgorithmException, IOException, InterruptedException {
		Path filePath = Paths.get(filename);
		if(!Files.exists(filePath)) { //NOTE: O ficheiro nao existe
			System.out.println("Error: File "+filename+" does not exist: ");
			return;
		}
		Long numberOfChunks = (Math.floorDiv(Files.size(filePath), 64000))+1;
		String fileID = this.getFileID(filename);
		LocalState.getInstance().getBackupFiles().put(fileID, new BackupFile(filename, Peer.id, replicationDegree));
		int chunkNo = 0;
		while(chunkNo < numberOfChunks) {
			
			AsynchronousFileChannel channel = AsynchronousFileChannel.open(filePath);
			ByteBuffer body = ByteBuffer.allocate(64000);
			int numberOfChunk = chunkNo;
			CompletionHandler<Integer, ByteBuffer> reader =new CompletionHandler<Integer, ByteBuffer>() {
				@Override
				public void completed(Integer result, ByteBuffer buffer) {
					//System.err.println("result = " + result);
	
					buffer.flip();
					byte[] data = new byte[buffer.limit()];
					buffer.get(data);
					//System.out.println(new String(data));
					buffer.clear();
					try {
						backupChunk(numberOfChunk, replicationDegree, data, fileID, filename, isEnhancement);
					} catch (UnsupportedEncodingException | InterruptedException e) {
						e.printStackTrace();
					} 
					
				}
	
				@Override
				public void failed(Throwable arg0, ByteBuffer arg1) {
					System.err.println("Error: Could not read!");
					
				}
				
			};
			channel.read(body, 64000*chunkNo, body, reader);
			chunkNo++;
		}//TODO: Enhancement backup
	}
	
	/**
	 * @param chunkNo
	 * @param replicationDegree
	 * @param bodyOfTheChunk
	 * @param fileID
	 * @param filename
	 * @param isEnhancement
	 * @throws InterruptedException
	 * @throws UnsupportedEncodingException
	 */
	public static void backupChunk(int chunkNo, int replicationDegree, byte[] bodyOfTheChunk, String fileID, String fileName, Boolean isEnhancement) throws InterruptedException, UnsupportedEncodingException {

		Chunk chunk = new Chunk(chunkNo, replicationDegree, (long) bodyOfTheChunk.length, Peer.id);
		//System.out.println("A Guardar file: "+fileID+" CHUNKNO: "+chunk.getID());
		LocalState.getInstance().saveChunk(fileID, fileName, Peer.id, replicationDegree, chunk);
		//System.out.println("SAVACHUNK!!!: "+fileID+" CHUNKNO: "+chunk.getID());
		LocalState.getInstance().decreaseReplicationDegree(fileID, chunk.getID(), Peer.id, Peer.id);
		double version = Peer.protocolVersion; //TODO: isEnhancement
		if(isEnhancement) {
			version = 1.1;
		}
		SendPutChunk subprotocol = new SendPutChunk(version, Peer.id, fileID, fileName, chunkNo, replicationDegree, bodyOfTheChunk);
		SingletonThreadPoolExecutor.getInstance().getThreadPoolExecutor().submit(subprotocol);
		return;
	}
		
	/* (non-Javadoc)
	 * @see server.InterfaceApp#deleteFile(java.lang.String, java.lang.Boolean)
	 */
	@Override
	public void deleteFile(String filename, Boolean isEnhancement) throws NoSuchAlgorithmException, IOException {
		String fileID = getFileID(filename);
		double version = Peer.protocolVersion;
		if(LocalState.getInstance().getBackupFiles().get(fileID)==null) {
			LocalState.getInstance().getBackupFiles().put(fileID, new BackupFile(filename, Peer.id, 0));
		}
		
		if(isEnhancement) { 
			version = 1.2;
			LocalState.getInstance().notifyItWasDeleted(fileID);
		}else {
			LocalState.getInstance().getBackupFiles().remove(fileID);
		}
		if(sendDeleteMessage(version, Peer.id, fileID) == -1) {
			System.err.println("Error: Could not send DELETE message.");
			return;
		}
	}	
	/**
	 * Generate a file ID
	 * @param filename - the filename
	 * @return Hexadecimal SHA-256 encoded fileID
	 * @throws IOException, NoSuchAlgorithmException
	 * */
	public String getFileID(String filename) throws IOException, NoSuchAlgorithmException {
		Path filePath = Paths.get(filename); //The filename, not FileID
		BasicFileAttributes attr = Files.readAttributes(filePath, BasicFileAttributes.class);
		//System.out.println("lastModifiedTime: " + attr.lastModifiedTime());
		MessageDigest digest = MessageDigest.getInstance("SHA-256");
		byte[] hash = digest.digest((filename + attr.lastModifiedTime()).getBytes(StandardCharsets.UTF_8));
		return DatatypeConverter.printHexBinary(hash);
	}


	/* (non-Javadoc)
	 * @see server.InterfaceApp#getFile(java.lang.String, java.lang.Boolean)
	 */
	@Override
	public void getFile(String filename, Boolean isEnhancement) throws NoSuchAlgorithmException, IOException {
		
		String fileID = getFileID(filename);
		Integer chunkNo = 0;
		long fileSize = (Long) Files.getAttribute(Paths.get(filename), "size");
		int totalNumChunks = (int) (Math.floorDiv(fileSize, 64000) + 1);//numero total de chunks que o file vai ter
		for(int i = 0; i < totalNumChunks; i++) {
			LocalState.getInstance().getBackupFiles().get(fileID).getChunks().get(chunkNo).setRestoreMode(State.RECEIVE);
			sendGetChunk(fileID, chunkNo,isEnhancement);
			chunkNo++;
		}
	}
	
	/* (non-Javadoc)
	 * @see server.InterfaceApp#getState()
	 */
	@Override
	public String getState() {
		return LocalState.getInstance().getStateFileInfo();
	}
	
	
	@Override
	public boolean reclaimStorage(int space) {
		if(LocalState.getInstance().setStorageCapacity(space)) {
			//apagar chunks
			ArrayList<Pair<String, Integer>> deletedChunks = LocalState.getInstance().manageStorage();//enviar uma mensagem REMOVED para cada chunk apagado
			for(Pair<String, Integer> pair : deletedChunks) {
				try {
					Files.delete(Peer.getP().resolve(pair.getL()+"_"+pair.getR()));
					if(sendRemovedMessage(Peer.protocolVersion, Peer.id, pair.getL(), pair.getR()) == -1) {
						System.err.println("Error: Could not send REMOVED message.");
						return false;
					}
				} catch (IOException e) {
					e.printStackTrace();
				} 
			}
				
			return true;
		}
		return false;//falso se nao teve de apagar chunks
	}

	/**
	 * @param fileID
	 * @param chunkNo
	 * @param isEnhancement
	 * @throws UnsupportedEncodingException
	 */
	private static void sendGetChunk(String fileID, Integer chunkNo, Boolean isEnhancement) throws UnsupportedEncodingException {
		String msg = null;
		msg = createGetChunkMessage(fileID, chunkNo,isEnhancement) ;
		ChannelMC.getInstance().sendMessage(msg.getBytes("ISO-8859-1"));
		System.out.println("SENT --> "+msg);//GETCHUNk
	}
	
	/**
	 * @param fileID
	 * @param chunkNo
	 * @param isEnhancement
	 * @return
	 */
	private static String createGetChunkMessage(String fileID, Integer chunkNo, Boolean isEnhancement) {
		double version = Peer.protocolVersion;
		if(isEnhancement) { version=1.1; }
		String msg = "GETCHUNK "+ version + " " + Peer.id + " " + fileID+ " " + chunkNo + " \r\n\r\n";
		return msg;
	}

	/**
	 * @return the p
	 */
	public static Path getP() {
		return p;
	}

	/**
	 * @param p the p to set
	 */
	public static void setP(Path p) {
		Peer.p = p;
	}


}