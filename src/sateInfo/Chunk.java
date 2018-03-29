package sateInfo;

import java.util.ArrayList;

public class Chunk {
	private int id = 0;
	private int replicationDeg = 0;
	private int currReplicationDeg = 0;
	private int size = 0;
	
	private boolean sentWithGetChunk =false;
	
	private ArrayList<Integer> peersStoring = new ArrayList<Integer>();
	
	public Chunk(int id, int replicationDeg, int size, int peerID) {
		this.id = id;
		this.replicationDeg = replicationDeg;
		this.size = size;
		peersStoring.add(peerID);
		this.currReplicationDeg++;
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
	public Chunk decreaseReplicationDeg(int peerID) {
		
		if(peersStoring.contains(peerID)) {
			peersStoring.remove((Integer) peerID);
			this.currReplicationDeg--;
		}
		return this;
	}
	
	/**
	 * verifies if  the actual replication degree of a chunk is different from the one that is desired
	 * @return
	 */
	public boolean desireReplicationDeg() {
		return this.replicationDeg <= this.currReplicationDeg;
	}

	/**
	 * Verifies if this peer is a new peer storing the chunk; if not, saves this peer id
	 * @param peerID peer who is now storing the chunk
	 * @return true if this peer is storing the chunk
	 */
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
	
	public void addNewPeer(int peerID) {
		peersStoring.add(peerID);
	}

	public boolean isReplicationDegreeZero() {
		return currReplicationDeg == 0;
	}
}
