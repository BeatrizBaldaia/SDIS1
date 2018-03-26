package sateInfo;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class LocalState {
	private static LocalState instance = null;
	
	/**
	 * maximum amount of disk space that can be used to store chunks (in Bytes)
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
			instance = new LocalState(65000, 0);
		}
		return instance;
	}
	
	/**
	 * 
	 * @return the storage capacity
	 */
	public int getStorageCapacity() {
		return storageCapacity;
	}
	
	/**
	 * 
	 * @return the used storage amount
	 */
	public int getUsedStorage() {
		return usedStorage;
	}
	
	/**
	 * Updates the used storage info after saving one more chunk
	 * @param size of the chunk saved
	 */
	public void setUsedStorage(int size) {
		usedStorage += size;
	}
	
	/**
	 * Saves the new chunk
	 * @param fileID
	 * @param pathName
	 * @param serviceID
	 * @param replicationDeg
	 * @param chunk
	 * @return false if the chunk already exists
	 */
	public boolean saveChunk(String fileID, String pathName, int serviceID, int replicationDeg, Chunk chunk) {
		System.err.println(fileID+"-->"+chunk.getID());
		//System.err.println("saveChunk Before: "+getBackupFiles().get(fileID).getChunks().size());
		if(getBackupFiles().computeIfAbsent(fileID, k -> new BackupFile(pathName, serviceID, replicationDeg).addChunk(chunk)) == null) {
			if(getBackupFiles().get(fileID).addChunk(chunk) == null) {//ja tinhamos o chunk guardado
				System.err.println("saveChunk false: "+getBackupFiles().get(fileID).getChunks().size());
				return false;
			}
		}
		System.err.println("saveChunk True: "+getBackupFiles().get(fileID).getChunks().size());
		return true;
	}
	public boolean updateReplicationInfo(int senderID, String fileID, int chunkID) {
		return getBackupFiles().get(fileID).updateReplicationInfo(chunkID, senderID);
	}
	
	public boolean seeIfAlreadySent(String fileID, int chunkID) {
		return getBackupFiles().get(fileID).seeIfAlreadySent(chunkID);
	}

	public void notifyThatItWasSent(String fileID, int chunkNo) {
		getBackupFiles().get(fileID).notifyThatItWasSent(chunkNo);		
	}

	/**
	 * @return the backupFiles
	 */
	public Map<String, BackupFile> getBackupFiles() {
		return backupFiles;
	}

	public void returnToFalse(int fileID, int chunkNo) {
		getBackupFiles().get(fileID).returnToFalse(chunkNo);		
	}

}
