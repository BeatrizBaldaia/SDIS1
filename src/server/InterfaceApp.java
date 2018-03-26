/**
 * 
 */
package server;

import java.io.IOException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.security.NoSuchAlgorithmException;

/**
 * @author anabela
 *
 */
public interface InterfaceApp extends Remote {
	public void backup(String filename, Integer replicationDegree) throws RemoteException, NoSuchAlgorithmException, IOException, InterruptedException;

	public void delete(String filename) throws NoSuchAlgorithmException, IOException, RemoteException;

	public void getFile(String filename) throws RemoteException, NoSuchAlgorithmException, IOException;
}
