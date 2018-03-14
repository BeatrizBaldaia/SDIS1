
package Initiator;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.UnicastRemoteObject;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import Message.ChannelMC;
import Message.ChannelMDB;
import Message.ChannelMDR;
import Server.InterfaceApp;
import Subprotocols.ChunkBackup;

public class Peer implements InterfaceApp {
	private static int protocolVersion;
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
			mc.createMulticastSocket(args[3], args[4]);
			mdb = ChannelMDB.getInstance();
			mdb.createMulticastSocket(args[5], args[6]);
			mdr = ChannelMDR.getInstance();
			mdr.createMulticastSocket(args[7], args[8]);
			
			mc.listen();
			mdb.listen();
			mdr.listen();
			
			

			/*ChunkBackup protocol = new ChunkBackup(mcc, mdb, id);

			protocol.send(0, replicationDegree, fileName, mdb_ip);*/
			
			 try {
		            Peer obj = new Peer();
		            Peer protocol = (Peer) UnicastRemoteObject.exportObject(obj, 0);

		            // Bind the remote object's stub in the registry
		            Registry registry = LocateRegistry.getRegistry();
		            registry.bind("PROTOCOL", protocol);

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
	public String createPutChunkMessage(double version, int senderID, int fileID, int chunkNo, int replicationDeg, byte [] body) throws UnsupportedEncodingException {
		String bodyStr = new String(body, "UTF-8"); // for UTF-8 encoding
		String msg = "PUTCHUNK "+ version + " " + senderID + " " + fileID + " " + chunkNo + " " + replicationDeg + " \r\n\r\n" + bodyStr;
		return msg;
	}
	
	public int sendPutChunkMessage(double version, int senderID, int fileID, int chunkNo, int replicationDeg, byte [] body) {
		
		String msg = null;
		try {
			msg = createPutChunkMessage(version, senderID, fileID, chunkNo, replicationDeg, body) ;
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return -1;
		}
		
		ChannelMDB.getInstance().sendMessage(msg.getBytes());
		return 0;
	}

	
	@Override
	public void backup(String filename, Integer replicationDegree) {
		// TODO Auto-generated method stub
		System.out.println("Chamou backup");
		
	}

}
