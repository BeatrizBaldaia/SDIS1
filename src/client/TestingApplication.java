package client;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

import server.InterfaceApp;

public class TestingApplication {
	public static void main(String[] args) {
		if (args.length != 1) {
			System.out.println("NOT ENOUGTH ARGUMENTS");
			return;
		}
		Integer protocol_ID = Integer.valueOf(args[0]);
		switch (protocol_ID) {
		case 0:{
			try {
				String filename = "a.png";
				Integer degree = 1;
				Registry registry = LocateRegistry.getRegistry(null);
				InterfaceApp protocol = (InterfaceApp) registry.lookup("PROTOCOL");
				protocol.backupFile(filename, degree);
				System.out.println("Returned");
			} catch (Exception e) {
				System.err.println("Client exception: " + e.toString());
				e.printStackTrace();
			}break;
		}
		case 1:{
			try {
				String filename = "a.png";
				//Integer degree = 2;
				Registry registry = LocateRegistry.getRegistry(null);
				InterfaceApp protocol = (InterfaceApp) registry.lookup("PROTOCOL");
				protocol.deleteFile(filename);
				System.out.println("Returned");
			} catch (Exception e) {
				System.err.println("Client exception: " + e.toString());
				e.printStackTrace();
			}break;
		}
		case 2:{
			try {
				String filename = "a.png";
				Registry registry = LocateRegistry.getRegistry(null);
				InterfaceApp protocol = (InterfaceApp) registry.lookup("PROTOCOL");
				protocol.getFile(filename);
				System.out.println("Returned");
			} catch (Exception e) {
				System.err.println("Client exception: " + e.toString());
				e.printStackTrace();
			}break;
		}
		default: {
			System.err.println("Error: Not a recognizable protocol.");
		}
		}

	}
}
