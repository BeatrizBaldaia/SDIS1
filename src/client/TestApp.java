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
		if (args.length < 3) {
			System.out.println("Error: Not enougth arguments.");
			return;
		}
		String peerAccessPoint = args[0];
		System.out.println("Peer Access Point: " + peerAccessPoint);
		String subprotocol = args[1];
		Registry registry;
		InterfaceApp protocol;
		try {
			registry = LocateRegistry.getRegistry(null);
			protocol = (InterfaceApp) registry.lookup("PROTOCOL");//TODO: AccessPoint
		} catch (RemoteException | NotBoundException e1) {
			e1.printStackTrace();
			return;
		}
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
				System.err.println("Implement RECLAIM");//TODO: reclaim
				break;
			}
			case "STATE":{
				System.err.println("Implement STATE");//TODO: state
				break;
			}
			default: {
				System.err.println("Error: Not a recognizable protocol.");
				return;
			}
		}
	}
}
