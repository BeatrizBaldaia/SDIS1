package subprotocols;

import java.io.UnsupportedEncodingException;
import java.util.Arrays;

import initiator.Peer;
import message.Parser;
import sateInfo.Chunk;
import sateInfo.LocalState;

public class Reclaiming implements Runnable{
	public double version = 0.0;
	public int senderID = 0;
	public String fileID = null;
	public String fileName = null;
	public int chunkNo = 0;
	private byte[] body = null;
	
	public Reclaiming (Parser parser) {
		version = parser.version;
		senderID = parser.senderID;
		fileID = parser.fileID;
		fileName = new String(parser.fileID);
		chunkNo = parser.chunkNo;
		this.body = Arrays.copyOf(body, body.length);
	}
	
	@Override
	public void run() {
		Chunk chunk = LocalState.getInstance().getBackupFiles().get(this.fileID).getChunks().get(this.chunkNo);
		if(chunk.getReclaimMode() == Chunk.State.ON) {
			chunk.setReclaimMode(Chunk.State.OFF);
			try {
				boolean isEnhancement = (this.version == 1.2) ? true : false;
				
				Peer.backupChunk(this.chunkNo, chunk.getReplicationDegree(), this.body, this.fileID, this.fileName, isEnhancement);
			} catch (UnsupportedEncodingException | InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
}
