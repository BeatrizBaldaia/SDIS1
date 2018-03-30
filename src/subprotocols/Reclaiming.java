package subprotocols;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.file.Files;
import java.util.Arrays;

import initiator.Peer;
import message.Parser;
import sateInfo.LocalState;
import server.Utils;

public class Reclaiming implements Runnable{
	public double version = 0.0;
	public int senderID = 0;
	public String fileID = null;
	public String fileName = null;
	public int chunkNo = 0;
	
	public Reclaiming (Parser parser) {
		version = parser.version;
		senderID = parser.senderID;
		fileID = parser.fileID;
		fileName = new String(parser.fileID);
		chunkNo = parser.chunkNo;
	}
	
	@Override
	public void run() {
		LocalState.getInstance().decreaseReplicationDegree(fileID, chunkNo, senderID, Peer.id);
		if(!LocalState.getInstance().getBackupFiles().get(fileID).getChunks().get(chunkNo).desireReplicationDeg()) { //se o replication degree esta abaixo do que e pedido
			try {
				Utils.randonSleep(Utils.TIME_MAX_TO_SLEEP);//TODO: interromper quando recebe mensagem PUTCHUNK
			} catch (InterruptedException e) {
				// nao envia PUTCHUNK
				e.printStackTrace();
			}
		}
		try {
			int replicationDegree = LocalState.getInstance().getBackupFiles().get(fileID).getChunks().get(chunkNo).getReplicationDeg();
			byte[] body = Files.readAllBytes(Peer.getP().resolve(fileID+"_"+chunkNo));
			//Peer.backupChunk(chunkNo, replicationDegree, body, fileID, null, false);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
