package subprotocols;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;

import initiator.Peer;
import message.ChannelMDR;
import message.Parser;
import sateInfo.Chunk;
import sateInfo.LocalState;

public class ChunkRestore implements Runnable {

	public double version = 0.0;
	public int senderID = 0;
	public String fileID = null;
	public int chunkNo = 0;
	public byte[] body = null;

	public ChunkRestore(Parser parser) {
		version = parser.version;
		senderID = parser.senderID;
		fileID = parser.fileID;
		chunkNo = parser.chunkNo;
	}
	@Override
	public void run() {
		Chunk chunk = LocalState.getInstance().getBackupFiles().get(this.fileID).getChunks().get(this.chunkNo);
		if(chunk.getRestoreMode() == Chunk.State.ON) {//enviar CHUNK msg
			try {
				sendChunkMessage();
			} catch (IOException e) {
				e.printStackTrace();
			}
			chunk.setRestoreMode(Chunk.State.OFF);
		} 
	}

	private void sendChunkMessage() throws IOException {
		String msg = null;
		msg = createChunkMessage();
		ChannelMDR.getInstance().sendMessage(msg.getBytes("ISO-8859-1"));
		System.out.println("SENT --> "+msg.split("\r\n")[0]);
	}

	private String createChunkMessage() throws IOException {
			Path filePath = Peer.getP().resolve(this.fileID+"_"+this.chunkNo);
			try {
				this.body = Files.readAllBytes(filePath);
			} catch (IOException e) {
				System.out.println("Couldn't read from file!");
				e.printStackTrace();
				return null;
			}
		if(this.version != 1) { //ENHANCEMENT
			
			ServerSocket machine = new ServerSocket(1040);
			byte[] data  = this.body;
			String address = machine.getInetAddress().getHostAddress()+":"+1040;
			this.body = address.getBytes();
			System.err.println(address);
			System.err.println("Socket address: "+this.body.toString());
			System.err.println("Socket address: "+data.toString());
			new Thread(() -> {
				try {
					Socket socket = machine.accept();
					DataOutputStream out = new DataOutputStream(socket.getOutputStream());
					out.write(data);
					out.close();
					machine.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}).start();
		}
		String bodyStr = new String(this.body,"ISO-8859-1"); // for "ISO-8859-1" encoding
		String msg = "CHUNK "+ this.version + " " + Peer.id + " " + this.fileID+ " " + chunkNo + " \r\n\r\n" + bodyStr;
		return msg;
	}
}
