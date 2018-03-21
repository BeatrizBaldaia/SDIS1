package sateInfo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class BackupFile {

	private String pathName = null;
	private int serviceID = 0;
	private int replicationDeg = 0;
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
	public void addChunk(Chunk chunk) {
		chunks.putIfAbsent(chunk.getID(), chunk);
	}

}
