package initiator;

import java.io.DataInputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.Socket;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import javax.xml.bind.DatatypeConverter;

import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;

import message.ChannelMC;
import message.ChannelMDB;
import message.ChannelMDR;
import message.Parser;
import sateInfo.BackupFile;
import sateInfo.Chunk;
import sateInfo.LocalState;
import server.InterfaceApp;

public class Peer implements InterfaceApp {
	private static double protocolVersion;
	public static int id;
	private static int serviceAccessPoint;
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
			serviceAccessPoint = Integer.parseInt(args[2]);
			
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
				Registry registry = LocateRegistry.getRegistry(1099);
				registry.rebind("PROTOCOL", protocol); //TODO: see diference bind/rebind

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
	public String createPutChunkMessage(double version, int senderID, String fileID, int chunkNo, int replicationDeg, byte [] body) throws UnsupportedEncodingException {
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
	public int sendPutChunkMessage(double version, int senderID, String fileID, int chunkNo, int replicationDeg, byte [] body) throws UnsupportedEncodingException {
		
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
		byte[] body;
		try {
			body = Files.readAllBytes(filePath);
		} catch (IOException e) {
			System.out.println("Error: Could not read from file!");
			e.printStackTrace();
			return;
		}
		String fileID = this.getFileID(filename);
		//System.out.println("FileID: "+fileID);
		int chunkNo = 0;
		while(body.length>=(64000*(chunkNo+1))) { //TODO: teste Muliple of 64
			byte[] bodyOfTheChunk = Arrays.copyOfRange(body, chunkNo*64000, (chunkNo+1)*64000);
	
			backupChunk(chunkNo, replicationDegree, bodyOfTheChunk, fileID, filename, isEnhancement);
			chunkNo++;

		}
		byte[] bodyOfTheChunk = Arrays.copyOfRange(body, chunkNo*64000, body.length);
		backupChunk(chunkNo, replicationDegree, bodyOfTheChunk, fileID, filename,isEnhancement);
		//TODO: Enhancement backup
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
	public void backupChunk(int chunkNo, int replicationDegree, byte[] bodyOfTheChunk, String fileID, String filename, Boolean isEnhancement) throws InterruptedException, UnsupportedEncodingException {
			//System.err.println("Going to backUp cunkN= "+chunkNo);
	
			Chunk chunk = new Chunk(chunkNo, replicationDegree, (long) bodyOfTheChunk.length, Peer.id);
			LocalState.getInstance().saveChunk(fileID, filename, Peer.id, replicationDegree, chunk);
			LocalState.getInstance().decreaseReplicationDegree(fileID, chunk.getID(), Peer.id);
			double version = Peer.protocolVersion; //TODO: isEnhancement
			if(this.sendPutChunkMessage(version, Peer.id, fileID, chunkNo, replicationDegree, bodyOfTheChunk) == -1) {
				System.err.println("Error: Could not send PUTCHUNK message.");
				return;
			}
			//System.err.println("bodyOfTheChunk.length: "+bodyOfTheChunk.length);
			for(int i = 0; i < 5; i++) {
				Thread.sleep(1000*((int)Math.pow(2,i)));
				if(LocalState.getInstance().getBackupFiles().get(fileID).desireReplicationDeg(chunk.getID())) return;
				if(this.sendPutChunkMessage(Peer.protocolVersion, Peer.id, fileID, chunkNo, replicationDegree, bodyOfTheChunk) == -1) {
					System.err.println("Error: Could not send PUTCHUNK message.");
					return;
				}
			}
			
			return;
		}
		
	/* (non-Javadoc)
	 * @see server.InterfaceApp#deleteFile(java.lang.String, java.lang.Boolean)
	 */
	@Override
	public void deleteFile(String filename, Boolean isEnhancement) throws NoSuchAlgorithmException, IOException {
		String fileID = getFileID(filename);
		double version = Peer.protocolVersion;
		LocalState.getInstance().notifyItWasDeleted(fileID);
		if(isEnhancement) { version = 1.2; }
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
		Chunk chunk = new Chunk(chunkNo, 0, (long) 0, Peer.id);
		LocalState.getInstance().saveChunk(fileID, filename, Peer.id, 0, chunk);
		sendGetChunk(fileID, chunkNo,isEnhancement);
		Path filepath = Peer.getP().resolve("restoreFile");
		Files.deleteIfExists(filepath);
		Files.createFile(filepath);
		
		//TODO: Enhancement getFile
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

	/**
	 * @param parser
	 * @throws IOException
	 */
	public static void restoreChunk(Parser parser) throws IOException {
	//	System.err.println("Restore");
		Boolean isEnhancement = false;
		if(parser.version != 1) { //Enhancements
			isEnhancement = true;
			String data = new String(parser.body, "ISO-8859-1");
			String[] elem = data.split(":");
			//System.err.println("Data :"+data);
//			System.err.println(elem[0]);
//			System.err.println(elem[1]);
			parser.body = new byte[64000];
			Socket socket = new Socket(elem[0], Integer.valueOf(elem[1]));//TODO: Socket Port
			DataInputStream input = new DataInputStream(socket.getInputStream());
			int length = 0;
			try {
				while(true) {
					byte b = input.readByte();
					parser.body[length]= b;
					length++;
				}
			} catch (EOFException e) {
				//System.err.println("ASSIM .." );
			}
			socket.close();
			//System.out.println("LEU: "+length);
			parser.body = Arrays.copyOfRange(parser.body, 0, length);
		}
		Chunk chunk = new Chunk(parser.chunkNo+1, 0, (long) 0, Peer.id);
		LocalState.getInstance().saveChunk(parser.fileName, null, Peer.id, 0, chunk);
		Path filepath = Peer.getP().resolve("restoreFile");
		FileOutputStream g = new FileOutputStream(filepath.toFile(),true);  //true --> append
		g.write(parser.body);
		g.close();
		//TODO: if two send the chunk?
//		System.err.println("Chunk length: "+parser.body.length);
		if(parser.body.length>=64000)
			sendGetChunk(parser.fileName, parser.chunkNo+1, isEnhancement);
	}
}