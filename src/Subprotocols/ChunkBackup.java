package Subprotocols;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.MulticastSocket;

public class ChunkBackup {
	private MulticastSocket mc;//control socket
	private MulticastSocket mdb;//data socket
	private int serverID;
	
	public ChunkBackup (MulticastSocket mc, MulticastSocket mdb, int serverID) {
		this.mc = mc;
		this.mdb = mdb;
		this.serverID = serverID;
	}

	public void send(int chunckID, int replicationDegree, String fileName) {
		
	}
	
	public void store() throws IOException {
		byte[] buf = new byte[256];
		DatagramPacket receivedPacket = new DatagramPacket(buf, buf.length);
		mdb.receive(receivedPacket);
		
		//Fazer o parse de buf; ler ate ao primeiro CRLF para o header
		String msg = new String(receivedPacket.getData());
		msg = msg.trim();
		String[] parts = msg.split(" ");
	}
}
