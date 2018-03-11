package Initiator;

import java.net.InetAddress;
import java.net.MulticastSocket;

import Subprotocols.ChunkBackup;

public class Peer {

	public static void main(String[] args) {
			if (args.length != 5) {
				System.out.println("Usage: java Peer <id> <MCC_ip> <MCC_port> <MDB_ip> <MDB_port> <file> <replicationDegree>");
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

			String fileName = args[5];
			int replicationDegree = Integer.parseInt(args[6]);

			ChunkBackup protocol = new ChunkBackup(mcc, mdb, peerID);

			protocol.send();
		}

}
