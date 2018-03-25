package sateInfo;

public class Chunk {
	private int id = 0;
	private int replicationDeg = 0;
	private int currReplicationDeg = 0;
	private int size = 0;
	
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
		this.replicationDeg++;
		return this;
	}
	
	/**
	 * decreases the current replication degree by one
	 * @return this chunk
	 */
	public Chunk decreaseReplicationDeg() {
		this.replicationDeg--;
		return this;
	}
	
	/**
	 * verifies if  the actual replication degree of a chunk is different from the one that is desired
	 * @return
	 */
	public boolean desireReplicationDeg() {
		return this.replicationDeg == this.currReplicationDeg;
	}
}
