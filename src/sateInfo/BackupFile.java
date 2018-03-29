package sateInfo;

import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;


public class BackupFile {

	private String pathName = null;
	private int serviceID = 0;
	private int replicationDeg = 0;
	private Map<Integer,Chunk> chunks = new ConcurrentHashMap<Integer, Chunk>();
	private Boolean wasDeleted = false;
	
	public BackupFile(String pathName, int serviceID, int replicationDeg) {
		this.pathName = pathName;
		this.serviceID = serviceID;
		this.replicationDeg = replicationDeg;
	}
	
	/**
	 * 
	 * @return the file pathname
	 */
	public String getPathName() {
		return this.pathName;
	}
	
	/**
	 * 
	 * @return the backup service id of the file
	 */
	public int getServiceID() {
		return this.serviceID;
	}
	
	/**
	 * 
	 * @return the desired replication degree
	 */
	public int getReplicationDegree() {
		return this.replicationDeg;
	}
	
	/**
	 * stores the info of one chunk of this file
	 * @param chunk
	 */
	public BackupFile addChunk(Chunk chunk) {
		if(getChunks().computeIfAbsent(chunk.getID(), k -> chunk) != null) {
			LocalState.getInstance().setUsedStorage(chunk.getSize());
			return this;
		}
		return null;
		
	}
	
	/**
	 * verifies if  the actual replication degree of a chunk is different from the one that is desired
	 * @return
	 */
	public boolean desireReplicationDeg(int chunkID) {
		return chunks.get(chunkID).desireReplicationDeg();
	}
	
	/**
	 * increases the replication degree if there is a new peer storing the chunk
	 * @param chunkID
	 * @param senderID
	 * @return
	 */
	public boolean updateReplicationInfo(int chunkID, int senderID) {
		return getChunks().get(chunkID).isNewPeerStoring(senderID);
	}

	public boolean seeIfAlreadySent(int chunkID) {
		return getChunks().get(chunkID).seeIfAlreadySent();
	}

	public void notifyThatItWasSent(int chunkNo) {
		getChunks().get(chunkNo).notifyThatItWasSent();		
	}

	public void returnToFalse(int chunkNo) {
		getChunks().get(chunkNo).returnToFalse();		
		
	}

	/**
	 * @return the chunks
	 */
	public Map<Integer,Chunk> getChunks() {
		return chunks;
	}

	/**
	 * @param chunks the chunks to set
	 */
	public void setChunks(Map<Integer,Chunk> chunks) {
		this.chunks = chunks;
	}
	/**
	 * increases by one the current replication degree
	 * @param chunkID
	 */
	public void increaseReplicationDegree(int chunkID) {
		chunks.get(chunkID).increaseReplicationDeg();
	}
	/**
	 * decreases by one the current replication degree
	 * @param chunkID
	 */
	public void decreaseReplicationDegree(int chunkID, int peerID) {
		LocalState.getInstance().setUsedStorage(-(chunks.get(chunkID).getSize()));//Frees storage
		chunks.get(chunkID).decreaseReplicationDeg(peerID);
	}
	
	public void decreaseReplicationDegree(int peerID) {
		for (Entry<Integer, Chunk> entry : chunks.entrySet()) {
			entry.getValue().decreaseReplicationDeg(peerID);
		}
	}
	
	/**
	 * Free storage
	 * @return
	 */
	public int deleteChunks() {
		int totalSpace = 0;

		for (ConcurrentHashMap.Entry<Integer, Chunk> entry : chunks.entrySet()) {
		    Chunk value = entry.getValue();
		    totalSpace += value.getSize();
		}
		
		return totalSpace;
	}
	
	public boolean isBackupInitiator() {
		return pathName != null;
	}

	public void notifyItWasDeleted() {
		wasDeleted = true;
	}

	public boolean isReplicationDegreeZero() {
		for (Entry<Integer, Chunk> entry : chunks.entrySet()) {
			if(!entry.getValue().isReplicationDegreeZero()) {
				return false;
			}
		}
		return true;
	}

	

}
