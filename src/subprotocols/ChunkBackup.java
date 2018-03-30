package subprotocols;

import java.io.UnsupportedEncodingException;
import message.*;

public class ChunkBackup implements Runnable {	

	public double version = 0.0;
	public int myID = 0;
	public String fileID = null;
	public int chunkNo = 0;


	public ChunkBackup (double version, int myID, String fileID, int chunkNo) {
		this.version = version;
		this.myID = myID;
		this.fileID = fileID;
		this.chunkNo = chunkNo;
	}

	public void sendConfirmation () throws InterruptedException, UnsupportedEncodingException  {
		String msg = "STORED "+ this.version + " " + this.myID + " " + this.fileID + " " + this.chunkNo + " \r\n\r\n";
		ChannelMC.getInstance().sendMessage(msg.getBytes("ISO-8859-1"));
		System.out.println("SENT --> "+ msg);
	}
	
	@Override
	public void run() {
		try {
			sendConfirmation();
		} catch (UnsupportedEncodingException | InterruptedException e) {
			e.printStackTrace();
		}//enviar sempre a mensagem store mesmo quando ja tinhamos este chunk guardado
		return;
	}
}
