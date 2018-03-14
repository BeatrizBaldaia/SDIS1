package Client;

import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

import Server.InterfaceApp;

public class TestingApplication {
	public static void main(String[] args) {
		try {
			String filename = null;
			Integer degree = null;
	        Registry registry = LocateRegistry.getRegistry(null);
	        InterfaceApp protocol = (InterfaceApp) registry.lookup("PROTOCOL");
	        protocol.backup(filename, degree);
	        System.out.println("Returned");
	    } catch (Exception e) {
	        System.err.println("Client exception: " + e.toString());
	        e.printStackTrace();
	    }
	}
}
