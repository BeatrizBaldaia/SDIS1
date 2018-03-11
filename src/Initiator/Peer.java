
package Initiator;

import java.io.IOException;
import java.net.InetAddress;
import java.net.MulticastSocket;

import Message.ChannelMC;
import Message.ChannelMDB;
import Message.ChannelMDR;
import Subprotocols.ChunkBackup;

public class Peer {
	private static int protocolVersion;
	private static int id;
	private static int serviceAccessPoint;
	
	private static ChannelMC mc;
	private static ChannelMDB mdb;
	private static ChannelMDR mdr;
	
	

	public static void main(String[] args) throws IOException {
			if (args.length != 9) {
				System.out.println("Usage: java Initiator.Peer <protocol_version> <server_id> <service_access_point> <MC_ip> <MC_port> <MDB_ip> <MDB_port> <MDR_ip> <MDR_port>");
				return;
			}

			protocolVersion = Integer.parseInt(args[0]);
			id = Integer.parseInt(args[1]);
			serviceAccessPoint = Integer.parseInt(args[2]);
			
			mc = ChannelMC.getInstance();
			mc.createMulticastSocket(args[3], args[4]);
			mdb = ChannelMDB.getInstance();
			mdb.createMulticastSocket(args[5], args[6]);
			mdr = ChannelMDR.getInstance();
			mdr.createMulticastSocket(args[7], args[8]);
			
			mc.listen();
			mdb.listen();
			mdr.listen();

			/*ChunkBackup protocol = new ChunkBackup(mcc, mdb, id);

			protocol.send(0, replicationDegree, fileName, mdb_ip);*/
		}

}
