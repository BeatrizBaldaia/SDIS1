package sateInfo;

import java.util.ArrayList;




public class Chunk {
	public enum State { OFF, ON, RECEIVE }
	private int id = 0;
	private int replicationDeg = 0;
	private State restoreMode = State.OFF;
	private State reclaimMode = State.OFF;
	
	private int currReplicationDeg = 0;
	private Long size = (long) 0;
	
	private boolean sentWithGetChunk =false;
	
	private ArrayList<Integer> peersStoring = new ArrayList<Integer>();
	

	public Chunk(int id, int replicationDeg, Long size2, int peerID) {
		this.id = id;
		this.replicationDeg = replicationDeg;
		this.size = size2;
		peersStoring.add(peerID);
		this.currReplicationDeg++;
	}
	
	/**
	 * 
	 * @return the restore mode: 
	 * 	OFF = not going to try sent the CHUNK msg;
	 *  ON = may try to send CHUNK msg after waiting random time;
	 *  RECEIVE = the one whi asked for restore.
	 */
	public State getRestoreMode() {
		return this.restoreMode;
	}
	
	/**
	 * 
	 * @return the reclaim mode: 
	 * 	OFF = not going to try sent the PUTCHUNK msg;
	 *  ON = may try to send PUTCHUNK msg after waiting random time;
	 *  CONTINUE = received a PUTCHUNK msg while waiting so is not going to send PUTCHUNK msg.
	 */
	public State getReclaimMode() {
		return this.reclaimMode;
	}
	
	/**
	 * Changes the current restore mode
	 * @param state
	 */
	public void setRestoreMode(State state) {
		this.restoreMode = state;
	}
	
	/**
	 * Changes the current reclaim mode
	 * @param state
	 */
	public void setReclaimMode(State state) {
		this.reclaimMode = state;
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
	public long getSize() {
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
		return this.replicationDeg < this.currReplicationDeg;
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

	
	/**
	 * 
	 * @return true if has exceeded the desired replication degree
	 */
	public boolean exceededDesiredReplicationDeg() {
		return this.currReplicationDeg > this.replicationDeg;
	}
	
	public int getexceededAmount() {
		return this.currReplicationDeg - this.replicationDeg;
	}
	public boolean isReplicationDegreeZero() {
		return currReplicationDeg <= 0;
	}
}
