package Subprotocols;

import java.net.MulticastSocket;

public class ChunkBackup {
	private MulticastSocket mc;
	private MulticastSocket mdb;
	private int serverID;
	
	public ChunkBackup (MulticastSocket mc, MulticastSocket mdb, int serverID) {
		this.mc = mc;
		this.mdb = mdb;
		this.serverID = serverID;
	}

	public void send(int chunckID, int replicationDegree, String fileName) {
		
	}
	
	public void store() {
		
	}
}
