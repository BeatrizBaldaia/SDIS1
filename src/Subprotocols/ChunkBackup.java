package Subprotocols;

import Message.*;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.MulticastSocket;
import java.nio.file.*;
import java.nio.file.Files;
import java.net.InetAddress;

public class ChunkBackup implements Runnable {
	private MulticastSocket mc;//control socket
	private MulticastSocket mdb;//data socket
	private int serverID;

	public ChunkBackup (MulticastSocket mc, MulticastSocket mdb, int serverID) {
		this.mc = mc;
		this.mdb = mdb;
		this.serverID = serverID;
	}

	public void send(int chunckID, int replicationDegree, String fileName, InetAddress mdb_ip) throws IOException  {
		Path fileName_path = Paths.get(fileName);
		byte[] data = Files.readAllBytes(fileName_path);
		String request = "PUTCHUNK 1.1 "+this.serverID+" "+fileName+" 0 "+replicationDegree+ " \r\n\r\n" + data;
		byte[] request_to_bytes = request.getBytes();
		DatagramPacket requestPacket = new DatagramPacket(request_to_bytes, request_to_bytes.length, mdb_ip,mdb.getLocalPort());
		mdb.setTimeToLive(1);
		//requestPacket.setSocketAddress(mdb.getLocalSocketAddress());
		System.out.println(" "+mdb.getInetAddress()+":"+mdb.getLocalPort());
		mdb.send(requestPacket);
		System.out.println("SENT");
	}
	public void run() {
		return;
	}
	public void store() throws IOException {
		byte[] buf = new byte[256+60*1000];
		DatagramPacket receivedPacket = new DatagramPacket(buf, buf.length);
		System.out.println("reply");
		mdb.receive(receivedPacket);
		System.out.println("receive");
		Parser parser = new Parser(receivedPacket.getData(),receivedPacket.getData().length);
		parser.parseHeader();

		Path fileName = Paths.get(parser.fileName);
		if(!Files.exists(fileName)) { //O CHUNk nao Existe

			byte[] data = parser.body;
			Files.write(fileName,data);

		}
		String reply = new String("STORED ");
		reply += parser.version + " " + this.serverID+ " " + parser.fileName+ " " + '0' /*chunk no*/ + " " + parser.CR + parser.LF+ parser.CR + parser.LF;
		System.out.println(reply);
		//Fazer o parse de buf; ler ate ao primeiro CRLF para o header
		// String msg = new String(receivedPacket.getData());
		// msg = msg.trim();
		// String[] parts = msg.split(" ");
	}
}
