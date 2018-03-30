package sateInfo;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import initiator.Peer;

/**
 * @author anabela
 *
 */
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

	private ConcurrentHashMap<String, BackupFile> backupFiles = new ConcurrentHashMap<String, BackupFile>();
	
	private Map<String, Pair<String,Integer> > restoring = new ConcurrentHashMap<String, Pair<String,Integer> >();
	
	/**
	 * @return the restoring
	 */
	public Map<String, Pair<String,Integer> > getRestoring() {
		return restoring;
	}

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
	 * @param l of the chunk saved
	 */
	public void setUsedStorage(long l) {
		usedStorage += l;
	}
	
	/**
	 * Saves the new chunk
	 * @param fileID
	 * @param pathName
	 * @param serviceID
	 * @param replicationDeg
	 * @param chunk
	 */
	
	public synchronized void saveChunk(String fileID, String pathName, int serviceID, int replicationdeg, Chunk chunk) {
		if(getBackupFiles().compute(fileID, (k,v) -> computeSaveChunk(k,v,pathName,serviceID,replicationdeg,chunk)) == null) {
			if(getBackupFiles().get(fileID).getChunks().get(chunk.getID())==null) {
				System.err.println("Chunk Nao salvado1 "+chunk.getID());
			} else {
				System.out.println("1GUARDAR CHUNK NO :"+getBackupFiles().get(fileID).getChunks().get(chunk.getID()).getID() );
			}
		}
		if(getBackupFiles().get(fileID).getChunks().get(chunk.getID())==null) {
			System.err.println("Chunk Nao salvado2 "+chunk.getID());
		} else {
			System.out.println("2GUARDAR CHUNK NO :"+getBackupFiles().get(fileID).getChunks().get(chunk.getID()).getID() );
		}
//		if(getBackupFiles().computeIfPresent(fileID, (k,v) -> v.addChunk(chunk)) == null) {
//			getBackupFiles().computeIfAbsent(fileID, k -> createNewBackupFile(k,pathName, serviceID, replicationDeg, chunk));
//			if(getBackupFiles().get(fileID).getChunks().get(chunk.getID())==null) {
//				System.err.println("Chunk Nao salvado1 "+chunk.getID());
//			} else {
//				System.out.println("GUARDAR CHUNK NO :"+getBackupFiles().get(fileID).getChunks().get(chunk.getID()).getID() );
//			}
//			return;
//		}
//		if(getBackupFiles().get(fileID).getChunks().get(chunk.getID())==null) {
//			System.err.println("Chunk Nao salvado2 "+chunk.getID());
//		} else {
//			System.out.println("GUARDADO CHUNK NO :"+getBackupFiles().get(fileID).getChunks().get(chunk.getID()).getID() );
//		}
//		return;
	}
	private BackupFile computeSaveChunk(String k, BackupFile v, String pathName, int serviceID, int replicationdeg,
			Chunk chunk) {
		BackupFile file = v;
		if(file == null) {
			System.err.println("UMA VEZ!!");
			file = new BackupFile(pathName, serviceID, replicationdeg);
		}
		file.addChunk(chunk);
		return file;
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
	private BackupFile createNewBackupFile(String fileID, String pathName, int serviceID, int replicationDeg, Chunk chunk) {
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
	
	/**
	 * Delete all information about one file of fileID; free space
	 * @param fileID
	 * @return
	 */
	public boolean deleteFileChunks(String fileID) {
		System.err.println("deleteFileChunks");
		BackupFile file = null; 
		if((file = backupFiles.get(fileID)) != null) {
			//System.err.println("If");
			int recoveredSpace = file.deleteChunks();
			if(recoveredSpace > 0) {
				//System.err.println("recover");
				this.usedStorage -= recoveredSpace;
				//File directory = new File(".");
				Path dir = Peer.getP();
				File directory = dir.toFile();
				String pattern = Peer.getP().toString()+File.separator+fileID + "*";
				PathMatcher matcher = FileSystems.getDefault().getPathMatcher("glob:" + pattern);
				File[] files = directory.listFiles();
				for(int i = 0; i<files.length; i++) {
					String filename = files[i].getName();
					//System.err.println("FILE: "+filename);
					Path name = Peer.getP().resolve(filename);
					//System.err.println("PATHS: "+name.toString());
					if (name != null && matcher.matches(name)) {
						//System.err.println("  Pertence");
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
	
	/**
	 * Sees if a CHUNK message was already sent
	 * @param fileID
	 * @param chunkID
	 * @return true if some peer has already sent a CHUNK message
	 */
	public boolean seeIfAlreadySent(String fileID, int chunkID) {
		return getBackupFiles().get(fileID).seeIfAlreadySent(chunkID);
	}
	
	/**
	 * Marks the chunk as having already been sent in a CHUNK message
	 * @param fileID
	 * @param chunkNo
	 */
	public void notifyThatItWasSent(String fileID, int chunkNo) {
		getBackupFiles().get(fileID).notifyThatItWasSent(chunkNo);
	}
	/**
	 * Marks the file as having already been deleted, in this peer
	 * @param fileID
	 */
	public void notifyItWasDeleted(String fileID) {
		getBackupFiles().get(fileID).notifyItWasDeleted();		
	}
	/**
	 * @return the backupFiles
	 */
	public Map<String, BackupFile> getBackupFiles() {
		return backupFiles;
	}
	
	/**
	 * Decreases the replication degree of a chunk and frees storage
	 * @param fileID
	 * @param chunkID who is going to have his replication degree decreased
	 * @param peerID
	 */
	public void decreaseReplicationDegree(String fileID, int chunkID, int peerID, int myID) {
		int freedStorage = backupFiles.get(fileID).decreaseReplicationDegree(chunkID, peerID);
		if(myID == peerID) {
			this.usedStorage -= freedStorage;
		}
	}
	/**
	 * Decreases the replication degree of the whole file
	 * @param fileID
	 * @param peerID
	 */
	public void decreaseReplicationDegree(String fileName, int peerID) {
		backupFiles.get(fileName).decreaseReplicationDegree(peerID);
	}
	
	/**
	 * Increases the replication degree of a chunk and  storage
	 * @param fileID
	 * @param chunkID who is going to have his replication degree increased
	 */
	public void increaseReplicationDegree(String fileID, int chunkID) {
		backupFiles.get(fileID).increaseReplicationDegree(chunkID);
	}

	/**
	 * Returns Boolean sentWithGetChunk to false, so that a new GETCHUNK may be sent
	 * @param fileName
	 * @param chunkNo
	 */
	public void returnToFalse(String fileName, int chunkNo) {
		getBackupFiles().get(fileName).returnToFalse(chunkNo);
	}
	
	/**
	 * Retrieve local service state information
	 * @return
	 */
	public String getStateFileInfo() {
		String info = "";
		for (String key : backupFiles.keySet()) {
			String fileInfo = "";
			if(backupFiles.get(key).isBackupInitiator()) {
				fileInfo = backupInitiatorInfo(backupFiles.get(key));
			} else {
				fileInfo = backupInitiatorInfo(backupFiles.get(key));
			}
			info += fileInfo + "\n";
		}
		
		info += storageCapacityInfo();
		
		return info;
	}
	
	/**
	 * For each file whose backup it has initiated:
	 * 	The file pathname
	 * 	The backup service id of the file
	 * 	The desired replication degree
	 * 	For each chunk of the file:
	 * 		- Its id
	 * 		- Its perceived replication degree
	 * @param file
	 * @return
	 */
	public String backupInitiatorInfo(BackupFile file) {
		String info = "I have initiated the backup of file:\n\t->Path Name: " + file.getPathName() + ";\n";
		info += "\t->Service ID: " + file.getServiceID() + "\n";
		info += "\t->Desired Replication Degree: " + file.getReplicationDegree() + "\n";
		info += "\t->Chunks:\n";
		for (Integer key : file.getChunks().keySet()) {
			Chunk chunk = file.getChunks().get(key);
			info += "\t   ID = " + chunk.getID() + " ; Perceived Replication Degree = " + chunk.getReplicationDegree() + "\n";
		}
	
		return info;
	}
	
	/**
	 * For each chunk it stores:
	 * 	- Its id
	 * 	- Its size (in KBytes)
	 * 	- Its perceived replication degree
	 * @param file 
	 * @return
	 */
	public String storedBackupChunksInfo(BackupFile file) {
		String info = "I'm storing the following chunks:\n";
		for (Integer key : file.getChunks().keySet()) {
			Chunk chunk = file.getChunks().get(key);
			info += "\tID = " + chunk.getID() + " ; Size = " + chunk.getSize() + " ; Perceived Replication Degree = " + chunk.getReplicationDegree() + "\n";
		}
		
		return info;
	}
	
	/**
	 * The peer's storage capacity, i.e. the maximum amount of disk space that can be used to store chunks,
	 *  and the amount of storage (both in KBytes) used to backup the chunks.
	 * @return
	 */
	public String storageCapacityInfo() {
		String info = "Storage Capacity:\n";
		info += "\t->Maximum Amount of disk Space: " + this.storageCapacity + "\n";
		info += "\t->Amount of Storage Used: " + this.usedStorage + "\n";
		
		return info;
	}
	
	/**
	 * Changes the maximum storage capacity
	 * @param space
	 * @return true if is using more space than the allowed
	 */
	public boolean setStorageCapacity(int space) {
		this.storageCapacity = space;
		return this.usedStorage > this.storageCapacity;
	}
	
	/**
	 * Deletes chunks in order to have an used storage amount equal or less than the
	 * maximum capacity
	 * @return A set of chunkID's, of one certain fileID, deleted
	 */
	public ArrayList<Pair<String, Integer>> manageStorage() {
		ArrayList<Pair<Pair<String, Integer>, Integer>> arr = new ArrayList<Pair<Pair<String, Integer>, Integer>>();
		ArrayList<Pair<String, Integer>> deletedChunks = new ArrayList<Pair<String, Integer>>();
		
		for(ConcurrentHashMap.Entry<String, BackupFile> entry : backupFiles.entrySet()) {
			arr.addAll(entry.getValue().getAllChunks(entry.getKey()));
		}
		Collections.sort(arr);		
		
		int i = 0;
		while(this.usedStorage > this.storageCapacity) {
			Pair<Pair<String, Integer>, Integer> pair = arr.get(i);
			String file_id = pair.getL().getL();
			Integer chunk_id = pair.getL().getR();

			deletedChunks.add(pair.getL());
			int freedStorage = (int) backupFiles.get(file_id).deleteChunk(chunk_id);
			//long freedStorage = backupFiles.get(file_id).getChunks().get(chunk_id).getSize();
			this.usedStorage -= freedStorage;
			 
		}
		
		return deletedChunks;
	}

	/**
	 * Checks if all peers have deleted a file
	 * @param fileName
	 * @return true if replication degree of all chunks is zero
	 */
	public boolean isReplicationDegreeZero(String fileName) {
		return this.backupFiles.get(fileName).isReplicationDegreeZero();
	}

}
