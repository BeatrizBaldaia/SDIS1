package sateInfo;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class LocalState {
	private static LocalState instance = null;
	
	/**
	 * maximum amount of disk space that can be used to store chunks (in KBytes)
	 * */
	private int storageCapacity;
	
	/**
	 * the amount of storage (in KBytes) used to backup the chunks
	 */
	private int usedStorage;
	
	private Map<String, BackupFile> backupFiles = new ConcurrentHashMap<String, BackupFile>();
	
	public LocalState(int storageCapacity, int usedStorage) {
		this.storageCapacity = storageCapacity;
		this.usedStorage = usedStorage;
		
	}
	
	public static LocalState getInstance() {
		if(instance == null) {
			instance = new LocalState(65, 0);
		}
		return instance;
	}
	
	public void saveChunk(String fileID, Chunk chunk) {
		/*backupFiles.computeIfPresent(fileID, )
		BackupFile file = backupFiles.get(fileID);
		file.addChunk(chunk);*/
	}

}
