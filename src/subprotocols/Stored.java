/**
 * 
 */
package subprotocols;

import java.util.Arrays;

import message.Parser;
import message.SingletonThreadPoolExecutor;

public class Stored implements Runnable {
	public double version = 0.0;
	public int senderID = 0;
	public String fileID = null;
	public String fileName = null;
	public int chunkNo = 0;
	public int replicationDeg = 0;
	public byte[] body = null;
	public int noReplications = 0;
	
	public Stored(Parser parser) {
		version = parser.version;
		senderID = parser.senderID;
		fileID = parser.fileName;
		//fileName = new String(parser.fileName, "ISO-8859-1");
		chunkNo = parser.chunkNo;
		replicationDeg = parser.replicationDeg;
		//body = Arrays.copyOf(parser.body, parser.body.length);
	}

	@Override
	public void run() {
		noReplications++;
	}

}
