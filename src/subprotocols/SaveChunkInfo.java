package subprotocols;

import java.util.Random;
import java.util.concurrent.TimeUnit;

import initiator.Peer;
import message.SingletonThreadPoolExecutor;
import sateInfo.BackupFile;
import sateInfo.Chunk;
import sateInfo.LocalState;

public class SaveChunkInfo implements Runnable {
	
	private double version = 0.0;
	private int myID = 0;
	private int senderID = 0;
	private String fileID = null;
	private int chunkNo = 0;
	private byte[] body;
	private int replicationDeg = 0;


	public SaveChunkInfo (double version, int myID, int senderID, String fileID, int chunkNo, byte[] body, int replicationDeg) {
		this.version = version;
		this.myID = myID;
		this.fileID = fileID;
		this.chunkNo = chunkNo;
		this.body = body;
		this.replicationDeg = replicationDeg;
		this.senderID = senderID;
	}
	@Override
	public void run() {
		BackupFile file = LocalState.getInstance().getBackupFiles().get(this.fileID);
		if(file == null) {
			Chunk chunk = new Chunk(this.chunkNo, this.replicationDeg, (long) this.body.length, myID);
			LocalState.getInstance().saveChunk(this.fileID, null, myID, this.replicationDeg, chunk);
			LocalState.getInstance().decreaseReplicationDegree(this.fileID, this.chunkNo, myID, myID);
		} else {
			Chunk chunk = file.getChunks().get(this.chunkNo);
			if(chunk == null) {
				chunk = new Chunk(this.chunkNo, this.replicationDeg, (long) this.body.length, myID);
				LocalState.getInstance().saveChunk(this.fileID, null, myID, this.replicationDeg, chunk);
				LocalState.getInstance().decreaseReplicationDegree(this.fileID, this.chunkNo, myID, myID);
			} else {
				chunk.setReplicationDeg(this.replicationDeg);
			}
			chunk.setReclaimMode(Chunk.State.OFF);
		}	
		
		Random r = new Random();
    	ChunkBackup subprotocol = new ChunkBackup(this.version, this.myID, this.senderID, this.fileID, this.chunkNo, this.body, this.replicationDeg);
    	SingletonThreadPoolExecutor.getInstance().getThreadPoolExecutor().schedule(subprotocol, (long) r.nextInt(400), TimeUnit.MILLISECONDS);
	}

}
