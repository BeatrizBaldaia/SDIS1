package subprotocols;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousFileChannel;
import java.nio.channels.CompletionHandler;
import java.nio.file.Paths;

import initiator.Peer;
import message.Parser;
import sateInfo.Chunk;
import sateInfo.LocalState;
import server.Utils;

public class Reclaiming implements Runnable{
	public double version = 0.0;
	public int senderID = 0;
	public String fileID = null;
	public String fileName = null;
	public int chunkNo = 0;

	
	public Reclaiming (Parser parser) {
		version = parser.version;
		senderID = parser.senderID;
		chunkNo = parser.chunkNo;
		fileID = parser.fileID;
		fileName = new String(parser.fileID);
		fileName = fileName + "_" + chunkNo;
		
		
	}
	
	@Override
	public void run() {
		if(LocalState.getInstance().getBackupFiles().get(fileID).getPathName() == null) {
			AsynchronousFileChannel channel;
			try {
				channel = AsynchronousFileChannel.open(Peer.getP().resolve(fileName));
				ByteBuffer body = ByteBuffer.allocate(Utils.MAX_LENGTH_CHUNK);
				CompletionHandler<Integer, ByteBuffer> reader =new CompletionHandler<Integer, ByteBuffer>() {
					@Override
					public void completed(Integer result, ByteBuffer buffer) {

						buffer.flip();
						byte[] data = new byte[buffer.limit()];
						buffer.get(data);
						buffer.clear();
						Chunk chunk = LocalState.getInstance().getBackupFiles().get(fileID).getChunks().get(chunkNo);
						if(chunk.getReclaimMode() == Chunk.State.ON) {
							chunk.setReclaimMode(Chunk.State.OFF);
							try {
								boolean isEnhancement = (version == 1.2) ? true : false;
								
								Peer.backupChunk(chunkNo, chunk.getReplicationDegree(), data, fileID, fileName, isEnhancement);
							} catch (UnsupportedEncodingException | InterruptedException e) {
								e.printStackTrace();
							}
						}
						
					}

					@Override
					public void failed(Throwable arg0, ByteBuffer arg1) {
						System.err.println("Error: Could not read!");
						
					}
					
				};
				channel.read(body, 0, body, reader);
			} catch (IOException e1) {
				e1.printStackTrace();
			}
		} else {
			AsynchronousFileChannel channel;
			try {
				channel = AsynchronousFileChannel.open(Paths.get(LocalState.getInstance().getBackupFiles().get(fileID).getPathName()));
				ByteBuffer body = ByteBuffer.allocate(Utils.MAX_LENGTH_CHUNK);
				CompletionHandler<Integer, ByteBuffer> reader =new CompletionHandler<Integer, ByteBuffer>() {
					@Override
					public void completed(Integer result, ByteBuffer buffer) {

						buffer.flip();
						byte[] data = new byte[buffer.limit()];
						buffer.get(data);
						buffer.clear();
						Chunk chunk = LocalState.getInstance().getBackupFiles().get(fileID).getChunks().get(chunkNo);
						if(chunk.getReclaimMode() == Chunk.State.ON) {
							chunk.setReclaimMode(Chunk.State.OFF);
							try {
								boolean isEnhancement = (version == 1.2) ? true : false;
								
								Peer.backupChunk(chunkNo, chunk.getReplicationDegree(), data, fileID, fileName, isEnhancement);
							} catch (UnsupportedEncodingException | InterruptedException e) {
								e.printStackTrace();
							}
						}
						
					}

					@Override
					public void failed(Throwable arg0, ByteBuffer arg1) {
						System.err.println("Error: Could not read!");
						
					}
					
				};
				channel.read(body, Utils.MAX_LENGTH_CHUNK*chunkNo, body, reader);
			} catch (IOException e1) {
				e1.printStackTrace();
			}
		}

		
	}
}
