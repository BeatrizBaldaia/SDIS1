package sateInfo;

import java.util.ArrayList;

public class Chunk {
	private int id = 0;
	private int replicationDeg = 0;
	private int currReplicationDeg = 0;
	private int size = 0;
	
	private boolean sentWithGetChunk =false;
	
	private ArrayList<Integer> peersStoring = new ArrayList();
	
	public Chunk(int id, int replicationDeg, int size) {
		this.id = id;
		this.replicationDeg = replicationDeg;
		this.size = size;
	}
	
	/**
	 * 
	 * @return the chunk id
	 */
	public int getID() {
		return this.id;
	}
	
	/**
	 * 
	 * @return the perceived replication degree
	 */
	public int getReplicationDegree() {
		return this.replicationDeg;
	}
	
	/**
	 * 
	 * @return the size (in KBytes)
	 */
	public int getSize() {
		return this.size;
	}
	
	/**
	 * increases the current replication degree by one
	 * @return this chunk
	 */
	public Chunk increaseReplicationDeg() {
		this.currReplicationDeg++;
		return this;
	}
	
	/**
	 * decreases the current replication degree by one
	 * @return this chunk
	 */
	public Chunk decreaseReplicationDeg() {
		this.currReplicationDeg--;
		return this;
	}
	
	/**
	 * verifies if  the actual replication degree of a chunk is different from the one that is desired
	 * @return
	 */
	public boolean desireReplicationDeg() {
		System.err.println(replicationDeg + "==" + this.currReplicationDeg);
		return this.replicationDeg == this.currReplicationDeg;
	}

	public boolean isNewPeerStoring(int peerID) {
		if(!peersStoring.contains(peerID)) {
			peersStoring.add(peerID);
			currReplicationDeg++;
			return true;
		}
		return false;
	}

	public boolean seeIfAlreadySent() {
		return sentWithGetChunk;
	}

	public void notifyThatItWasSent() {
		sentWithGetChunk = true;
	}

	public void returnToFalse() {
		sentWithGetChunk = false;
	}
}
