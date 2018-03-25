package sateInfo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class BackupFile {

	private String pathName = null;
	private int serviceID = 0;
	private int replicationDeg = 0;
	private int currReplicationDeg = 0;
	private Map<Integer,Chunk> chunks = new ConcurrentHashMap<Integer, Chunk>();
	
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
		if(chunks.computeIfAbsent(chunk.getID(), k -> chunk.increaseReplicationDeg()) != null) {
			this.currReplicationDeg++;
			LocalState.getInstance().setUsedStorage(chunk.getSize());
			return this;
		}
		
		return null;
		
	}
	
	/**
	 * verifies if  the actual replication degree of a chunk is different from the one that is desired
	 * @return
	 */
	public boolean desireReplicationDeg() {
		return this.replicationDeg == this.currReplicationDeg;
	}
	
	public boolean updateReplicationInfo(int chunkID, int senderID) {
		return chunks.get(chunkID).isNewPeerStoring(senderID);
	}

}
