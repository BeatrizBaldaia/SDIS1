/**
 * 
 */
package server;

import java.io.IOException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.security.NoSuchAlgorithmException;


public interface InterfaceApp extends Remote {
	/**
	 * Backup a chunk
	 * @param filename
	 * @param replicationDegree
	 * @param isEnhancement
	 * @throws RemoteException
	 * @throws NoSuchAlgorithmException
	 * @throws IOException
	 * @throws InterruptedException
	 */
	public void backupFile(String filename, Integer replicationDegree, Boolean isEnhancement) throws RemoteException, NoSuchAlgorithmException, IOException, InterruptedException;
		
	/**
	 * A file is deleted from its home file system
	 * @param filename
	 * @param isEnhancement
	 * @throws NoSuchAlgorithmException
	 * @throws IOException
	 */
	public void deleteFile(String filename, Boolean isEnhancement) throws NoSuchAlgorithmException, IOException, RemoteException;

	/**
	 * Retrieve local service state information
	 * @return
	 */
	public String getState() throws RemoteException;

	/**
	 * Recover all the chunks of a file
	 * @param filename
	 * @param isEnhancement
	 * @throws RemoteException
	 * @throws NoSuchAlgorithmException
	 * @throws IOException
	 */
	public void getFile(String filename, Boolean isEnhancement) throws RemoteException, NoSuchAlgorithmException, IOException;
	
	/**
	 * Managing the disk space reserved for the backup service
	 * @param space available for backup
	 * @return
	 */
	public boolean reclaimStorage(int space) throws RemoteException;

}
