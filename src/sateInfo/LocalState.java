package sateInfo;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.Paths;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import initiator.Peer;

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
			instance = new LocalState(65000000, 0);
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
	 */
	public void saveChunk(String fileID, String pathName, int serviceID, int replicationDeg, Chunk chunk) {
		if(getBackupFiles().computeIfPresent(fileID, (k,v) -> v.addChunk(chunk)) == null) {
			getBackupFiles().put(fileID, createNewBackupFile(fileID,pathName, serviceID, replicationDeg, chunk));
			return;
		};	
		return;
	}
	/**
	 * Creates a new BackupFile object to be saved in the hashmap
	 * @param fileID
	 * @param pathName
	 * @param serviceID
	 * @param replicationDeg
	 * @param chunk
	 * @return
	 */
	public BackupFile createNewBackupFile(String fileID, String pathName, int serviceID, int replicationDeg, Chunk chunk) {
		BackupFile file = new BackupFile(pathName, serviceID, replicationDeg);
		file.addChunk(chunk);
		return file;
	}
	/**
	 * Updates the current replication degree related to a file
	 * @param senderID
	 * @param fileID
	 * @param chunkID
	 * @return
	 */
	public boolean updateReplicationInfo(int senderID, String fileID, int chunkID) {
		System.out.println("Recebeu fileID = " + fileID + ", mas so temos guardadas as chaves:");
		for (String key : backupFiles.keySet()) {
		    System.out.println(key + " " + backupFiles.get(key).getChunks().size());
		}
		return getBackupFiles().get(fileID).updateReplicationInfo(chunkID, senderID);
	}
	
	public boolean deleteFileChunks(String fileID) {
		//TODO: Enhancement delete
		//System.err.println("deleteFileChunks");
		BackupFile file = null; 
		if((file = backupFiles.get(fileID)) != null) {
			int recoveredSpace = file.deleteChunks();
			if(recoveredSpace > 0) {
				this.usedStorage -= recoveredSpace;
				//File directory = new File(".");
				Path dir = Peer.getP();
				File directory = dir.toFile();
				String pattern = Peer.getP().toString()+File.pathSeparator+fileID + "*";//TODO: windons
				PathMatcher matcher = FileSystems.getDefault().getPathMatcher("glob:" + pattern);
				File[] files = directory.listFiles();
				for(int i = 0; i<files.length; i++) {
					String filename = files[i].getName();
					//System.err.println("FILE: "+filename);
					Path name = Peer.getP().resolve(filename);
					//System.err.println("PATHS: "+name.toString());
					if (name != null && matcher.matches(name)) {
						System.err.println("  Pertence");
						try {
							Files.delete(name);
						} catch (IOException e) {
							System.err.println("Error: Could not delete file: "+name);
							e.printStackTrace();
						}
					}
				}
			}
			backupFiles.remove(fileID);
			return true;
		}
		
		return false;
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
	
	public void decreaseReplicationDegree(String fileID, int chunkID) {
		backupFiles.get(fileID).decreaseReplicationDegree(chunkID);
	}
	public void increaseReplicationDegree(String fileID, int chunkID) {
		backupFiles.get(fileID).increaseReplicationDegree(chunkID);
	}

	public void returnToFalse(String fileName, int chunkNo) {
		getBackupFiles().get(fileName).returnToFalse(chunkNo);		
	}

}
