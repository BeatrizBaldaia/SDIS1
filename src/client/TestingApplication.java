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
				String filename = "teste.txt";
				Integer degree = 2;
				Registry registry = LocateRegistry.getRegistry(null);
				InterfaceApp protocol = (InterfaceApp) registry.lookup("PROTOCOL");
				protocol.backup(filename, degree);
				System.out.println("Returned");
			} catch (Exception e) {
				System.err.println("Client exception: " + e.toString());
				e.printStackTrace();
			}break;
		}
		case 1:{
			try {
				String filename = "teste.txt";
				Integer degree = 2;
				Registry registry = LocateRegistry.getRegistry(null);
				InterfaceApp protocol = (InterfaceApp) registry.lookup("PROTOCOL");
				protocol.delete(filename, degree);
				System.out.println("Returned");
			} catch (Exception e) {
				System.err.println("Client exception: " + e.toString());
				e.printStackTrace();
			}break;
		}
		}

	}
}
