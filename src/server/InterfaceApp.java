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
	public void backupFile(String filename, Integer replicationDegree, Boolean isEnhancement) throws RemoteException, NoSuchAlgorithmException, IOException, InterruptedException;
	
	public void deleteFile(String filename, Boolean isEnhancement) throws NoSuchAlgorithmException, IOException;

	public String getState();

	public void getFile(String filename, Boolean isEnhancement) throws RemoteException, NoSuchAlgorithmException, IOException;

}
