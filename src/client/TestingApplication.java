package client;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

import server.InterfaceApp;

public class TestingApplication {
	public static void main(String[] args) {
		if (args.length != 1) {
			System.out.println("Error: Not enougth arguments.");
			return;
		}
		String filename = "a.png";
		Integer degree = 1;
		Integer protocol_ID = Integer.valueOf(args[0]);
		try {
			Registry registry = LocateRegistry.getRegistry(null);
			InterfaceApp protocol = (InterfaceApp) registry.lookup("PROTOCOL");
			switch (protocol_ID) {
			case 0: {
				protocol.backupFile(filename, degree);
				System.out.println("Returned");
				break;
			}
			case 1: {
				protocol.deleteFile(filename);
				System.out.println("Returned");
				break;
			}
			case 2: {
				protocol.getFile(filename);
				System.out.println("Returned");
				break;
			}
			default: {
				System.err.println("Error: Not a recognizable protocol.");
			}
			}
		} catch (Exception e) {
			System.err.println("Client exception: " + e.toString());
			e.printStackTrace();
		}

	}
}
