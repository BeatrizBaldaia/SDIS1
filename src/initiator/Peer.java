package initiator;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Map;

import javax.xml.bind.DatatypeConverter;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
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
			
			setP(Paths.get("peer_"+id));
			if(!Files.exists(getP()))
				Files.createDirectory(getP());
			
			mc = ChannelMC.getInstance();
			mc.createMulticastSocket(args[3], args[4], id);
			mdb = ChannelMDB.getInstance();
			mdb.createMulticastSocket(args[5], args[6], id);
			mdr = ChannelMDR.getInstance();
			mdr.createMulticastSocket(args[7], args[8], id);
			
			mc.listen();
			mdb.listen();
			mdr.listen();
			
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
	
	public byte[] getFileBody(String fileName) throws IOException { //Deprecated
		String pathStr = "..//data/" + fileName;
		Path path = Paths.get(pathStr);
		byte[] data = Files.readAllBytes(path);
		return data;
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
		System.err.println("bodyStr.lenght:"+bodyStr.length());
		System.err.println("bodyStr.getBytes.size:"+bodyStr.getBytes("ISO-8859-1").length);
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
	public String createDeleteMessage(double version, int senderID, String fileID) throws UnsupportedEncodingException {
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
public int sendDeleteMessage(double version, int senderID, String fileID) throws UnsupportedEncodingException {
		
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

@Override
public void backupFile(String filename, Integer replicationDegree) throws NoSuchAlgorithmException, IOException, InterruptedException {
	Path filePath = Paths.get(filename);
	if(!Files.exists(filePath)) { //NOTE: O ficheiro nao existe
		System.out.println("File does not exist: "+ filename);
		return;
	}
	byte[] body;
	try {
		body = Files.readAllBytes(filePath);
	} catch (IOException e) {
		System.out.println("Couldn't read from file!");
		e.printStackTrace();
		return;
	}
	String fileID = this.getFileID(filename);
	System.out.println("FileID: "+fileID);
	int chunkNo = 0;
	while(body.length>=(64000*(chunkNo+1))) { //TODO: teste Muliple of 64
		byte[] bodyOfTheChunk = Arrays.copyOfRange(body, chunkNo*64000, (chunkNo+1)*64000);

		backupChunk(chunkNo, replicationDegree, bodyOfTheChunk, fileID, filename);
		chunkNo++;
	}
	byte[] bodyOfTheChunk = Arrays.copyOfRange(body, chunkNo*64000, body.length);
	backupChunk(chunkNo, replicationDegree, bodyOfTheChunk, fileID, filename);
	//TODO: Enhancement backup
}

public void backupChunk(int chunkNo, int replicationDegree, byte[] bodyOfTheChunk, String fileID, String filename) throws InterruptedException, UnsupportedEncodingException {
		System.err.println("Going to backUp cunkN= "+chunkNo);

		Chunk chunk = new Chunk(chunkNo, replicationDegree, bodyOfTheChunk.length, Peer.id);
		LocalState.getInstance().saveChunk(fileID, filename, Peer.id, replicationDegree, chunk);
		LocalState.getInstance().decreaseReplicationDegree(fileID, chunk.getID(), Peer.id);
		if(this.sendPutChunkMessage(Peer.protocolVersion, Peer.id, fileID, chunkNo, replicationDegree, bodyOfTheChunk) == -1) {
			System.err.println("Error: Could not send PUTCHUNK message.");
			return;
		}
		System.err.println("bodyOfTheChunk.length: "+bodyOfTheChunk.length);
		for(int i = 1; i <= 5; i++) {
			Thread.sleep(1000*i);
			if(LocalState.getInstance().getBackupFiles().get(fileID).desireReplicationDeg(chunk.getID())) return;
			if(this.sendPutChunkMessage(Peer.protocolVersion, Peer.id, fileID, chunkNo, replicationDegree, bodyOfTheChunk) == -1) {
				System.err.println("Error: Could not send PUTCHUNK message.");
				return;
			}
		}
		
		return;
	}
	
@Override
public void deleteFile(String filename) throws NoSuchAlgorithmException, IOException {
	String fileID = getFileID(filename);
	if(sendDeleteMessage(Peer.protocolVersion, Peer.id, fileID) == -1) {
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


	@Override
	public void getFile(String filename) throws NoSuchAlgorithmException, IOException {
		
		String fileID = getFileID(filename);
		Integer chunkNo = 0; //TODO: implement chunks
		sendGetChunk(fileID, chunkNo);
		Path filepath = Peer.getP().resolve("restoreFile");
		Files.deleteIfExists(filepath);
		Files.createFile(filepath);
		
		//TODO: guardar em file
		//TODO: Enhancement getFile
	}
	
	@Override
	public String getState() {
		return LocalState.getInstance().getStateFileInfo();
	}

	private static void sendGetChunk(String fileID, Integer chunkNo) throws UnsupportedEncodingException {
		String msg = null;
		msg = createGetChunkMessage(fileID, chunkNo) ;
		ChannelMC.getInstance().sendMessage(msg.getBytes("ISO-8859-1"));
		System.out.println("SENT --> "+msg);//GETCHUNk
	}
	
	private static String createGetChunkMessage(String fileID, Integer chunkNo) {
		String msg = "GETCHUNK "+ Peer.protocolVersion + " " + Peer.id + " " + fileID+ " " + chunkNo + " \r\n\r\n";
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

	public static void restoreChunk(Parser parser) throws IOException {
		System.err.println("Restore");
		Path filepath = Peer.getP().resolve("restoreFile");
		FileOutputStream g = new FileOutputStream(filepath.toFile(),true);  //true --> append
		g.write(parser.body);
		g.close();
		//TODO: if two send the chunk?
		System.err.println("Chunk length: "+parser.body.length);
		if(parser.body.length>=64000)
			sendGetChunk(parser.fileName, parser.chunkNo+1);
	}
}