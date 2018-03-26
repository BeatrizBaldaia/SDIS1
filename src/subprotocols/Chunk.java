package subprotocols;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Random;

import message.ChannelMC;
import message.ChannelMDR;
import message.Parser;
import sateInfo.LocalState;
import server.Utils;

public class Chunk implements Runnable {

	public double version = 0.0;
	public int senderID = 0;
	public String fileID = null;
	public int chunkNo = 0;
	public byte[] body = null;

	public Chunk(Parser parser) {
		version = parser.version;
		senderID = parser.senderID;
		fileID = parser.fileName;
		chunkNo = parser.chunkNo;
	}

	@Override
	public void run() {
		try {
			Utils.randonSleep(Utils.TIME_MAX_TO_SLEEP);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		if(LocalState.getInstance().seeIfAlreadySent(fileID, chunkNo)) return;
		try {
			System.err.println("SEND CHUNK");
			sendChunkMessage();
			LocalState.getInstance().notifyThatItWasSent(fileID, chunkNo);
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
	}

	private void sendChunkMessage() throws UnsupportedEncodingException {
		String msg = null;
		msg = createChunkMessage();
		ChannelMDR.getInstance().sendMessage(msg.getBytes());
		System.out.println("SENT --> "+msg);
	}

	private String createChunkMessage() throws UnsupportedEncodingException {
		Path filePath = Paths.get(this.fileID+"_"+this.chunkNo);
		try {
			this.body = Files.readAllBytes(filePath);
		} catch (IOException e) {
			System.out.println("Couldn't read from file!");
			e.printStackTrace();
			return null;
		}
		String bodyStr = new String(this.body, "UTF-8"); // for UTF-8 encoding
		String msg = "CHUNK "+ this.version + " " + this.senderID + " " + this.fileID+ " " + chunkNo + " \r\n\r\n" + bodyStr;
		return msg;
	}
}
