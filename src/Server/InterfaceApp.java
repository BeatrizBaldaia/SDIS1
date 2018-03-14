/**
 * 
 */
package Server;

import java.rmi.Remote;

/**
 * @author anabela
 *
 */
public interface InterfaceApp extends Remote {
	public void backup(String filename, Integer replicationDegree);
}
