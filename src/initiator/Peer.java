
package initiator;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

import javax.xml.bind.DatatypeConverter;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;

import message.ChannelMC;
import message.ChannelMDB;
import message.ChannelMDR;
import sateInfo.BackupFile;
import sateInfo.Chunk;
import sateInfo.LocalState;
import server.InterfaceApp;

public class Peer implements InterfaceApp {
	private static double protocolVersion;
	private static int id;
	private static int serviceAccessPoint;
	
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
	
	public byte[] getFileBody(String fileName) throws IOException {
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
		String bodyStr = new String(body, "UTF-8"); // for UTF-8 encoding
		String msg = "PUTCHUNK "+ version + " " + senderID + " " + fileID+ " " + chunkNo + " " + replicationDeg + " \r\n\r\n" + bodyStr;
		return msg;
	}
	
	public int sendPutChunkMessage(double version, int senderID, String fileID, int chunkNo, int replicationDeg, byte [] body) {
		
		String msg = null;
		try {
			msg = createPutChunkMessage(version, senderID, fileID, chunkNo, replicationDeg, body) ;
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return -1;
		}
		
		ChannelMDB.getInstance().sendMessage(msg.getBytes());
		System.out.println("SENT --> "+msg);
		return 0;
	}

	@Override
	public void backup(String filename, Integer replicationDegree) throws NoSuchAlgorithmException, IOException, InterruptedException {
		Path filePath = Paths.get(filename);
		if(!Files.exists(filePath)) { //NOTE: O ficheiro nao existe
			System.out.println("File does not exist: "+ filename);
			return;
		}
		//TODO: whay not send this one in LOCALSTATE?
		//BackupFile file = new BackupFile(filename, Peer.id, replicationDegree);
		byte[] body;
		try {
			body = Files.readAllBytes(filePath);
			//System.out.println("Body"+ new String(body));
		} catch (IOException e) {
			System.out.println("Couldn't read from file!");
			e.printStackTrace();
			return;
		}
		String fileID = this.getFileID(filename);
		System.out.println("FileID: "+fileID);
		int chunkNo = 0;
		//TODO: Separete in chunks
		Chunk chunk = new Chunk(chunkNo, replicationDegree, 64);//estou a assumir que este chunk e de tamanho 64
		LocalState.getInstance().saveChunk(fileID, filename, Peer.id, replicationDegree, chunk);
		LocalState.getInstance().decreaseReplicationDegree(fileID, chunk.getID());
		//TODO: se ao fim de um segundo nao receber n (sendo n = replicationDeg) o peer volta a enviar; passadas 5 tentativas, caga-se
		//se ao fim de 1 segundo o BackupFile.desireReplicationDeg() for false, reenvia
		if(this.sendPutChunkMessage(Peer.protocolVersion, Peer.id, fileID, chunkNo, replicationDegree, body)==-1) {
			System.out.println("Couldn't send putchunk!");
			return;
		}
//		for(int i = 1; i <= 5; i++) {
//			Thread.sleep(1000*i);
//			//TODO: descomment  See previous todos
//			//if(BackupFile.desireReplicationDeg()) return;
//			if(this.sendPutChunkMessage(Peer.protocolVersion, Peer.id, fileID, chunkNo, replicationDegree, body) == -1) {
//				System.err.println("Error: Could not send PUTCHUNK message.");
//				return;
//			}
//		}
		
		//TODO: Wait for responses.
		return;
	}
	/**
	 * Generate a file ID
	 * @param filename - the filename
	 * @return Hexadecimal SHA-256 encoded fileID
	 * @throws IOException, NoSuchAlgorithmException
	 * */
	public String getFileID(String filename) throws IOException, NoSuchAlgorithmException {
		Path filePath = Paths.get(filename);
		BasicFileAttributes attr = Files.readAttributes(filePath, BasicFileAttributes.class);
		//System.out.println("lastModifiedTime: " + attr.lastModifiedTime());
		MessageDigest digest = MessageDigest.getInstance("SHA-256");
		byte[] hash = digest.digest((filename + attr.lastModifiedTime()).getBytes(StandardCharsets.UTF_8));
		return DatatypeConverter.printHexBinary(hash);
	}

	@Override
	public void delete(String filename) throws NoSuchAlgorithmException, IOException {
		String fileID = getFileID(filename);
		sendDeleteMessage(fileID);
	}
	public int sendDeleteMessage(String fileID) {
		String msg = null;
		msg = createDeleteMessage(fileID) ;
		ChannelMC.getInstance().sendMessage(msg.getBytes());
		System.out.println("SENT --> "+msg);
		return 0;
	}

	private String createDeleteMessage(String fileID) {
		String msg = "DELETE "+ Peer.protocolVersion + " " + Peer.id + " " + fileID+ " \r\n\r\n";
		return msg;
	}

	@Override
	public void getFile(String filename) throws NoSuchAlgorithmException, IOException {
		String fileID = getFileID(filename);
		Integer chunkNo = 0; //TODO: implement chunks
		sendGetChunk(fileID, chunkNo);
		System.err.println("Sent getFile");
	}

	private void sendGetChunk(String fileID, Integer chunkNo) {
		String msg = null;
		msg = createGetChunkMessage(fileID, chunkNo) ;
		ChannelMC.getInstance().sendMessage(msg.getBytes());
		System.out.println("SENT --> "+msg);
	}

	private String createGetChunkMessage(String fileID, Integer chunkNo) {
		String msg = "GETCHUNK "+ Peer.protocolVersion + " " + Peer.id + " " + fileID+ " " + chunkNo + " \r\n\r\n";
		return msg;
	}
}