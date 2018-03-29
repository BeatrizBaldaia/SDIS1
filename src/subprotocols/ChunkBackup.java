package subprotocols;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.file.*;
import java.nio.file.Files;
import java.util.Arrays;
import initiator.Peer;
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
		fileID = parser.fileID;
		fileName = new String(parser.fileID);
		chunkNo = parser.chunkNo;
		replicationDeg = parser.replicationDeg;
		body = Arrays.copyOf(parser.body, parser.body.length);
	}

	public void sendConfirmation () throws InterruptedException, UnsupportedEncodingException  {
		Utils.randonSleep(Utils.TIME_MAX_TO_SLEEP);
		String msg = "STORED "+ this.version + " " + Peer.id + " " + this.fileID + " " + this.chunkNo + " \r\n\r\n";
		ChannelMC.getInstance().sendMessage(msg.getBytes("ISO-8859-1"));
		System.out.println("SENT --> "+ msg);
	}
	
	@Override
	public void run() {
		try {
			if((body.length + LocalState.getInstance().getUsedStorage()) <= LocalState.getInstance().getStorageCapacity()) {
				store();
			}
		} catch (IOException | InterruptedException e) {
			e.printStackTrace();
		}
		return;
	}

	public void store() throws IOException, InterruptedException {
		Utils.randonSleep(Utils.TIME_MAX_TO_SLEEP);
		Chunk chunk = new Chunk(chunkNo, replicationDeg, (long) body.length, Peer.id);
		LocalState.getInstance().saveChunk(fileID, null, senderID, replicationDeg, chunk);

		Path filePath = Peer.getP().resolve(fileName + "_" + chunkNo);
		if(!Files.exists(filePath)) { //NOTE: O CHUNk nao Existe
			System.out.println("Criar ficheiro: "+filePath);
			Files.createFile(filePath);
			Files.write(filePath,body);
		}
		
		
		sendConfirmation();//enviar sempre a mensagem store mesmo quando ja tinhamos este chunk guardado
	}

}
