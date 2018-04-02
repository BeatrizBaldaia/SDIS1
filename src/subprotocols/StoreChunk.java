package subprotocols;

import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousFileChannel;
import java.nio.channels.CompletionHandler;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;

import initiator.Peer;
import message.Parser;
import sateInfo.LocalState;
import server.Utils;

public class StoreChunk implements Runnable {
	private Parser parser;
	
	public StoreChunk(Parser parser) {
		this.parser = parser;
	}

	@Override
	public void run() {
		try {
			if(parser.version != 1.0) { //com enhancement
				getChunkWithTCP();
			}
			
			Path filepath = Peer.getP().resolve("restoreFile-"+LocalState.getInstance().getBackupFiles().get(parser.fileID).getPathName());
			
			try {
				Files.createFile(filepath);
			} catch(FileAlreadyExistsException e) {
			}
			
			
			AsynchronousFileChannel channel;
			channel = AsynchronousFileChannel.open(filepath,StandardOpenOption.WRITE);

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
			byte[] body = parser.body;
			ByteBuffer src = ByteBuffer.allocate(body.length);
			src.put(body);
			src.flip();
			channel.write(src, parser.chunkNo*Utils.MAX_LENGTH_CHUNK, src, writter);
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}
	
	public void getChunkWithTCP() throws NumberFormatException, UnknownHostException, IOException {
		String data = new String(this.parser.body, Utils.ENCODING_TYPE);
		String[] elem = data.split(":");
		this.parser.body = new byte[Utils.MAX_LENGTH_CHUNK];
		Socket socket = new Socket(elem[0], Integer.valueOf(elem[1]));
		DataInputStream input = new DataInputStream(socket.getInputStream());
		int length = 0;
		try {
			while(true) {
				byte b = input.readByte();
				this.parser.body[length]= b;
				length++;
			}
		} catch (EOFException e) { 
			System.out.println("All of the chunk's bytes read.");
		}
		socket.close();
		this.parser.body = Arrays.copyOfRange(this.parser.body, 0, length);
	}
	
}
