package NonInitiator;

import java.io.IOException;
import java.net.InetAddress;
import java.net.MulticastSocket;

import Subprotocols.ChunkBackup;

public class Peer {

	public static void main(String[] args) throws IOException {
		if (args.length != 5) {
			System.out.println("Usage: java Peer <id> <MCC_ip> <MCC_port> <MDB_ip> <MDB_port>");
			return;
		}

		int peerID = Integer.parseInt(args[0]);

		InetAddress mcc_ip = InetAddress.getByName(args[1]);
		int mcc_port = Integer.parseInt(args[2]);

		InetAddress mdb_ip = InetAddress.getByName(args[3]);
		int mdb_port = Integer.parseInt(args[4]);

		MulticastSocket mcc = new MulticastSocket(mcc_port);
		mcc.joinGroup(mcc_ip);

		MulticastSocket mdb = new MulticastSocket(mdb_port);
		mdb.joinGroup(mdb_ip);

		/*ChunkBackup protocol = new ChunkBackup(mcc, mdb, peerID);

		protocol.store();*/
	}
}
