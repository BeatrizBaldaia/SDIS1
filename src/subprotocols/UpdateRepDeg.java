package subprotocols;

import sateInfo.LocalState;

public class UpdateRepDeg implements Runnable {
	private int senderID;
	private String fileID;
	private int chunkNo;
	
	public UpdateRepDeg(int senderID, String fileID, int chunkNo) {
		this.senderID = senderID;
		this.fileID = fileID;
		this.chunkNo = chunkNo;
	}


	@Override
	public void run() {
		System.out.println("Recebeu do peer " + this.senderID + " msg STORE para o ficheiro " + this.fileID + ", chunk " + this.chunkNo);
		LocalState.getInstance().updateReplicationInfo(this.senderID, this.fileID, this.chunkNo);		
	}
}
