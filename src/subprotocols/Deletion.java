package subprotocols;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;

import initiator.Peer;
import message.ChannelMC;
import message.Parser;
import sateInfo.LocalState;

public class Deletion implements Runnable {
	public String fileID = null;
	public Double version = null;
		
	public Deletion(Parser parser) {
		fileID = parser.fileName;
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
		//TODO: Enhancement delete
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
