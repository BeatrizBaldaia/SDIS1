package subprotocols;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Random;

import message.*;

public class ChunkBackup implements Runnable {	
	public double version = 0.0;
	public int senderID = 0;
	public String fileID = null;
	public String fileName = null;
	public int chunkNo = 0;
	public int replicationDeg = 0;
	public byte[] body = null;
	
	public ChunkBackup (Parser parser) {
		version = parser.version;
		senderID = parser.senderID;
		fileID = parser.fileName;
		fileName = new String(parser.fileName);
		chunkNo = parser.chunkNo;
		replicationDeg = parser.replicationDeg;
		body = Arrays.copyOf(parser.body, parser.body.length);
	}

	public void sendConfirmation () throws InterruptedException  {
		Random r = new Random();
		Thread.sleep(r.nextInt(400));
		String msg = "STORED "+ this.version + " " + this.senderID + " " + this.fileID + " " + this.chunkNo + " \r\n\r\n";
		ChannelMC.getInstance().sendMessage(msg.getBytes());
		System.out.println("SENT --> "+ msg);
	}
	
	@Override
	public void run() {
		try {
			store();
		} catch (IOException | InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return;
	}
	
	public void store() throws IOException, InterruptedException {
		//TODO: DO NOT STORE IN THE SAME SERVER
		Path filePath = Paths.get(fileName + "_" + chunkNo);
		if(!Files.exists(filePath)) { //NOTE: O CHUNk nao Existe
			System.out.println("Criar ficheiro: "+filePath);
			Files.createFile(filePath);
			Files.write(filePath,body);
		}
		sendConfirmation();
	}

}
