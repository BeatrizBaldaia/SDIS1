package subprotocols;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousFileChannel;
import java.nio.channels.CompletionHandler;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import initiator.Peer;
import message.*;
import sateInfo.BackupFile;
import sateInfo.Chunk;
import sateInfo.LocalState;

public class ChunkBackup implements Runnable {	

	public double version = 0.0;
	public int myID = 0;
	private int senderID = 0;
	public String fileID = null;
	public int chunkNo = 0;
	private byte[] body;
	private int replicationDeg = 0;


	public ChunkBackup (double version, int myID, int senderID, String fileID, int chunkNo, byte[] body, int replicationDeg) {
		this.version = version;
		this.myID = myID;
		this.fileID = fileID;
		this.chunkNo = chunkNo;
		this.body = body;
		this.replicationDeg = replicationDeg;
		this.senderID = senderID;
	}

	public void sendConfirmation () throws InterruptedException, UnsupportedEncodingException  {
		String msg = "STORED "+ this.version + " " + this.myID + " " + this.fileID + " " + this.chunkNo + " \r\n\r\n";
		ChannelMC.getInstance().sendMessage(msg.getBytes("ISO-8859-1"));
		System.out.println("SENT --> "+ msg);
	}
	
	@Override
	public void run() {
		if((body.length + LocalState.getInstance().getUsedStorage()) <= LocalState.getInstance().getStorageCapacity()) {
			if(this.version == 1.1) {//enhancement
				System.err.println("BACKUP ENHANCEMENT");
				BackupFile file = LocalState.getInstance().getBackupFiles().get(this.fileID);
				if(file != null) {
					Chunk chunk = file.getChunks().get(this.chunkNo);
					if(chunk != null) {
						if(chunk.desireReplicationDeg()) {
							System.err.println("NAO FAZER STORE do chunk" + chunkNo + "porque ja temos o rep. deg. pedido");
							//LocalState.getInstance().getBackupFiles().remove(this.fileID);
							return;
						}
					}
				}
			}
			try {
				storeChunk();
			} catch (IOException e) {
				e.printStackTrace();
			}
			try {
				sendConfirmation();
			} catch (UnsupportedEncodingException | InterruptedException e) {
				e.printStackTrace();
			}//enviar sempre a mensagem store mesmo quando ja tinhamos este chunk guardado
		}
		
		return;
	}
	/**
     * Stores the chunk if it doesn't exists
     * @param parser
     * @throws IOException
     */
    public void storeChunk() throws IOException {
    	byte[] body = Arrays.copyOf(this.body, this.body.length);
    	Chunk chunk = new Chunk(chunkNo, this.replicationDeg, (long) body.length, Peer.id);
		LocalState.getInstance().saveChunk(this.fileID, null, this.senderID, this.replicationDeg, chunk);

		Path filePath = Peer.getP().resolve(this.fileID + "_" + this.chunkNo);
		if(!Files.exists(filePath)) { //NOTE: O CHUNk nao Existe
			System.out.println("Criar ficheiro: "+filePath);
			Files.createFile(filePath);
			AsynchronousFileChannel channel = AsynchronousFileChannel.open(filePath,StandardOpenOption.WRITE);
			CompletionHandler<Integer, ByteBuffer> writter = new CompletionHandler<Integer, ByteBuffer>() {
				@Override
				public void completed(Integer result, ByteBuffer buffer) {
					System.out.println("Finished writing!");
				}
	
				@Override
				public void failed(Throwable arg0, ByteBuffer arg1) {
					System.err.println("Error: Could not write!");
					
				}
				
			};
			ByteBuffer src = ByteBuffer.allocate(this.body.length);
			src.put(this.body);
			src.flip();
			channel.write(src, 0, src, writter);
		}
    }
}
