package client;

import java.io.IOException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.security.NoSuchAlgorithmException;

import server.InterfaceApp;

public class TestApp {
	public static void main(String[] args) throws RemoteException, NoSuchAlgorithmException, IOException, InterruptedException {
		if (args.length < 2) {
			System.out.println("Usage: java TestApp <peer_ap> <sub_protocol> [<opnd_1> [<opnd_2>]]");
			return;
		}
		String peerAccessPoint = args[0];
		String[] elems = peerAccessPoint.split(":");
		String host = elems[0];
		Integer port = Integer.valueOf(elems[1]);
		String name = elems[2];
		String subprotocol = args[1];
		Registry registry;
		InterfaceApp protocol;
		try {
			registry = LocateRegistry.getRegistry(host, port);
			protocol = (InterfaceApp) registry.lookup(name);
		} catch (RemoteException | NotBoundException e1) {
			e1.printStackTrace();
			return;
		}
		System.out.println("Connected to "+name+"!");
		Boolean isEnhancement = false;
		switch (subprotocol) {
			case "BACKUPENH":{
				isEnhancement = true;
			}	
			case "BACKUP":{
				String filename = args[2];
				Integer degree = Integer.valueOf(args[3]);
				protocol.backupFile(filename, degree, isEnhancement);
				System.out.println("Returned: Backup Done!");
				break;
			}
			case "RESTOREENH":{
				isEnhancement = true;
			}
			case "RESTORE":{
				String filename = args[2];
				protocol.getFile(filename,isEnhancement);
				System.out.println("Returned: Restore Done!");
				break;
			}
			case "DELETEENH":{
				isEnhancement = true;
			}
			case "DELETE":{
				String filename = args[2];
				protocol.deleteFile(filename,isEnhancement);
				System.out.println("Returned: Delete Done!");
				break;
			}
			case "RECLAIM":{
				Integer space = Integer.valueOf(args[2]);
				protocol.reclaimStorage(space);
				System.out.println("Returned: Reclaim Done!");
				break;
			}
			case "STATE":{
				String state = protocol.getState();
				System.out.println(state);
				System.out.println("Returned: State Done!");
				break;
			}
			default: {
				System.err.println("Error: Not a recognizable protocol.");
				return;
			}
		}
	}
}