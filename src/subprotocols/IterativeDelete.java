package subprotocols;

import sateInfo.LocalState;

public class IterativeDelete implements Runnable {
	private int senderID;
	private String fileID;
	
	public IterativeDelete(int senderID, String fileID) {
		this.senderID = senderID;
		this.fileID = fileID;
	}

	@Override
	public void run() {
		if(LocalState.getInstance().wasFileDeleted(this.fileID)) {
			if(LocalState.getInstance().getBackupFiles().get(this.fileID) != null) {
				LocalState.getInstance().decreaseReplicationDegree(this.fileID, this.senderID);
				if(LocalState.getInstance().isReplicationDegreeZero(this.fileID)) {
					LocalState.getInstance().getBackupFiles().remove(this.fileID);
				}
			}
		}
		
	}

}
