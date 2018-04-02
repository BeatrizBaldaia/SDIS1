package subprotocols;

import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import message.ChannelMDB;
import message.SingletonThreadPoolExecutor;
import sateInfo.LocalState;
import server.Utils;

public class SendPutChunk implements Runnable {

	private double version = 0.0;
	private int senderID = 0;
	private String fileID = null;
	//private String fileName = null;
	private int chunkNo = 0;
	private int replicationDeg = 0;
	private byte[] body = null;
	private int tries = 0;
	
	public SendPutChunk (double version, int senderID, String fileID, String fileName, int chunkNo, int replicationDeg, byte[] body) {
		this.version = version;
		this.senderID = senderID;
		this.fileID = fileID;
		//this.fileName = new String(fileName);
		this.chunkNo = chunkNo;
		this.replicationDeg = replicationDeg;
		this.body = Arrays.copyOf(body, body.length);
	}
	@Override
	public void run() {
		if(LocalState.getInstance().getBackupFiles().get(fileID).desireReplicationDeg(chunkNo)) {
			return;
		}
		if(tries == 5) {
			System.err.println("Error: Could not send PUTCHUNK message.");
			return;
		}
		try {
			sendPutChunkMessage(version, senderID, fileID, chunkNo, replicationDeg, body);
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		SingletonThreadPoolExecutor.getInstance().getThreadPoolExecutor().schedule(this, (long) (Math.pow(2, tries) * 1000) , TimeUnit.MILLISECONDS);
		tries++;
	}
	
	/**
	 * 
	 * @param version of the protocol
	 * @param senderID who is going to send the message
	 * @param fileID ; file identifier for the backup service
	 * @param chunkNo (chunkNo + fileID = specific chunk in a file)
	 * @param replicationDeg ; replication degree of the chunk
	 * @param body ; file data
	 * @return the PUTCHUNK message to be sent
	 * @throws UnsupportedEncodingException 
	 */
	public static String createPutChunkMessage(double version, int senderID, String fileID, int chunkNo, int replicationDeg, byte [] body) throws UnsupportedEncodingException {
		String bodyStr = new String(body, Utils.ENCODING_TYPE);
		String msg = "PUTCHUNK "+ version + " " + senderID + " " + fileID+ " " + chunkNo + " " + replicationDeg + " \r\n\r\n" + bodyStr;
		return msg;
	}
	
	/**
	 * sends the PUTCHUNK message to the MDB channel
	 * @param version
	 * @param senderID
	 * @param fileID
	 * @param chunkNo
	 * @param replicationDeg
	 * @param body
	 * @return different of 0 when error 
	 * @throws UnsupportedEncodingException 
	 */
	public static int sendPutChunkMessage(double version, int senderID, String fileID, int chunkNo, int replicationDeg, byte [] body) throws UnsupportedEncodingException {
		
		String msg = null;
		try {
			msg = createPutChunkMessage(version, senderID, fileID, chunkNo, replicationDeg, body) ;
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
			return -1;
		}
		
		ChannelMDB.getInstance().sendMessage(msg.getBytes(Utils.ENCODING_TYPE));
		System.out.println("SENT --> "+ msg.split("\r\n")[0]);//PUTCHUNK
		return 0;
	}
}
