package subprotocols;

import java.io.File;
import java.io.FileFilter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;

import message.Parser;

public class Deletion implements Runnable {
	public String fileID = null;
	
	public Deletion(Parser parser) {
		fileID = parser.fileName;
	}

	@Override
	public void run() {
		File directory = new File(".");
		String pattern = fileID+"*";
		PathMatcher matcher = FileSystems.getDefault().getPathMatcher("regex:" + pattern);
		File[] files = directory.listFiles();
		for(int i = 0; i<files.length; i++) {
			String filename = files[i].getName();
			Path name = Paths.get(filename);
			if (name != null && matcher.matches(name)) {
				try {
					Files.delete(name);
				} catch (IOException e) {
					System.out.println("Couldn't delete file: "+name);
					e.printStackTrace();
				}
			}
		}
	}

}
