package subprotocols;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;

import initiator.Peer;
import message.Parser;
import sateInfo.LocalState;

public class Deletion implements Runnable {
	public String fileID = null;
		
	public Deletion(Parser parser) {
		fileID = parser.fileName;
	}
	@Override
	public void run() {
		LocalState.getInstance().deleteFileChunks(fileID);
		//TODO: Enhancement delete
	}

}
