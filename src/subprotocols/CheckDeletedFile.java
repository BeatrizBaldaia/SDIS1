package subprotocols;

import java.io.UnsupportedEncodingException;

import initiator.Peer;
import sateInfo.LocalState;

public class CheckDeletedFile implements Runnable {

	private int myID;
	private double version;
	private String fileID;
	
	public CheckDeletedFile(int myID, double version, String fileID) {
		this.myID = myID;
		this.version = version;
		this.fileID = fileID;
		
	}
	@Override
	public void run() {
		if(LocalState.getInstance().getBackupFiles().get(this.fileID) != null) {
			if(LocalState.getInstance().wasFileDeleted(this.fileID)) {
				try {
					if(Peer.sendDeleteMessage(this.version, this.myID, this.fileID) == -1) {
						System.err.println("Error: Could not send DELETE message.");
						return;
					}
				} catch (UnsupportedEncodingException e) {
					e.printStackTrace();
				}
			}
		}		
	}

}
