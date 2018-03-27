package subprotocols;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;

import initiator.Peer;
import message.Parser;

public class Deletion implements Runnable {
	public String fileID = null;
		
	public Deletion(Parser parser) {
		fileID = parser.fileName;
	}

	@Override
	public void run() {
		File directory = new File(".");
		String pattern = fileID + "*";
		PathMatcher matcher = FileSystems.getDefault().getPathMatcher("glob:" + pattern);
		File[] files = directory.listFiles();
		for(int i = 0; i<files.length; i++) {
			String filename = files[i].getName();
			Path name = Peer.getP().resolve(filename);
			if (name != null && matcher.matches(name)) {
				try {
					Files.delete(name);
				} catch (IOException e) {
					System.err.println("Error: Could not delete file: "+name);
					e.printStackTrace();
				}
			}
		}
		//TODO: Enhancement delete
	}

}
