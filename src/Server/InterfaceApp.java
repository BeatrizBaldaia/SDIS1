/**
 * 
 */
package Server;

import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * @author anabela
 *
 */
public interface InterfaceApp extends Remote {
	public void backup(String filename, Integer replicationDegree) throws RemoteException;
}
