package Subprotocols;

import Message.*;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.MulticastSocket;
import java.nio.file.*;
import java.nio.file.Files;
import java.util.Arrays;
import java.net.InetAddress;

public class ChunkBackup implements Runnable {	
	public double version = 0.0;
	public int senderID = 0;
	public int fileID = 0;
	public String fileName = null;
	public int chunkNo = 0;
	public int replicationDeg = 0;
	public byte[] body = null;
	

	public ChunkBackup (Parser parser) {
		version = parser.version;
		senderID = parser.senderID;
		fileID = parser.fileID;
		fileName = new String(parser.fileName);
		chunkNo = parser.chunkNo;
		replicationDeg = parser.replicationDeg;
		body = Arrays.copyOf(parser.body, parser.body.length);
	}

	public void sendConfirmation ()  {
		/*Path fileName_path = Paths.get(fileName);
		byte[] data = Files.readAllBytes(fileName_path);*/
		String msg = "STORED "+ this.version + " " + this.senderID + " " + this.fileID + " " + this.chunkNo + " \r\n\r\n";
		ChannelMDB.getInstance().sendMessage(msg.getBytes());
		System.out.println("SENT");
	}
	public void run() {
		try {
			store();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return;
	}
	public void store() throws IOException {
		
		Path filePath = Paths.get(fileName);
		if(!Files.exists(filePath)) { //O CHUNk nao Existe
			Files.write(filePath,body);
		}
		
		sendConfirmation();
		//Fazer o parse de buf; ler ate ao primeiro CRLF para o header
		// String msg = new String(receivedPacket.getData());
		// msg = msg.trim();
		// String[] parts = msg.split(" ");
	}
}
