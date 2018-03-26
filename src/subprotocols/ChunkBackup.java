package subprotocols;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Random;

import message.*;
import sateInfo.Chunk;
import sateInfo.LocalState;
import server.Utils;

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
		Utils.randonSleep(Utils.TIME_MAX_TO_SLEEP);
		String msg = "STORED "+ this.version + " " + this.senderID + " " + this.fileID + " " + this.chunkNo + " \r\n\r\n";
		ChannelMC.getInstance().sendMessage(msg.getBytes());
		System.out.println("SENT --> "+ msg);
	}
	
	@Override
	public void run() {
		try {
			if((body.length + LocalState.getInstance().getUsedStorage()) <= LocalState.getInstance().getStorageCapacity()) {
				store();
			}
		} catch (IOException | InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return;
	}
	
	public void store() throws IOException, InterruptedException {
		System.err.println("Gets to store!");
		Chunk chunk = new Chunk(chunkNo, replicationDeg, body.length);
		if(LocalState.getInstance().saveChunk(fileID, fileName + "_" + chunkNo, senderID, replicationDeg, chunk)) {
			Path filePath = Paths.get(fileName + "_" + chunkNo);
			if(!Files.exists(filePath)) { //NOTE: O CHUNk nao Existe
				System.out.println("Criar ficheiro: "+filePath);
				Files.createFile(filePath);
				Files.write(filePath,body);
			}
		}
		
		Random r = new Random();
		Thread.sleep(r.nextInt(400));
		sendConfirmation();//enviar sempre a mensagem store mesmo quando ja tinhamos este chunk guardado
	}

}
