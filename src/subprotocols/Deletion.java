package subprotocols;

import java.io.UnsupportedEncodingException;
import initiator.Peer;
import message.ChannelMC;
import message.Parser;
import sateInfo.LocalState;

public class Deletion implements Runnable {
	public String fileID = null;
	public Double version = null;
		
	public Deletion(Parser parser) {
		fileID = parser.fileID;
		version = parser.version;
	}
	@Override
	public void run() {
		System.err.println("DELECTION PROTOCOL");
		LocalState.getInstance().deleteFileChunks(fileID);
		if(this.version == 1.2) { //Enhancement
			try {
				sendDeletedMessage();
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			}
		}
	}
	private void sendDeletedMessage() throws UnsupportedEncodingException {
		String msg = null;
		msg = createDeletedMessage();
		ChannelMC.getInstance().sendMessage(msg.getBytes("ISO-8859-1"));
		System.out.println("SENT --> "+msg);//DELETED
	}
	private String createDeletedMessage() {
		String msg = "DELETED "+ version + " " + Peer.id + " " + fileID+ " \r\n\r\n";
		return msg;
	}
}
