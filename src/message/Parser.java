package message;

import java.io.ByteArrayInputStream;

public class Parser {
	private ByteArrayInputStream stream = null;
	private String [] header = null;

	public String messageType = null;
	public double version = 0.0;
	public int senderID = 0;
	public int fileID = 0;
	public String fileName = null;
	public int chunkNo = 0;
	public int replicationDeg = 0;
	public byte[] body = null;

	 /**
     * Carriage return.
     */
    public static final byte CR = 0xD;

    /**
     * Line feed
     */
    public static final byte LF = 0xA;

    public Parser(byte[] message, int size) {
    	stream = new ByteArrayInputStream(message, 0, size);
    }

    /**
     * Parses the header.
     *
     * @param stream Stream that represents the message being parsed.
     * @return -1 for error.
     */
    public int parseHeader() {
    	StringBuilder sb = new StringBuilder();
    	byte byteRead;

        while ((byteRead = (byte) stream.read()) != CR) {
        	sb.append((char) byteRead);
        }

        if((byte) stream.read() != LF) {
        	return -1;
        }
        if((byte) stream.read() != CR) {
        	return -1;
        }
        if((byte) stream.read() != LF) {
        	return -1;
        }

        header = sb.toString().trim().split(" ");

        return verifyMessageType();
    }

    /**
     * Stores the protocol version
     * @return -1 if error
     */
    public int getVersion() {
    	if(header[1].length() != 3) {
    		System.out.println("This field should be in the format <n>'.'<m>");
    		return -1;
    	}
    	version = Double.parseDouble(header[1]);
    	return 0;
    }
    
    /**
     * Stores the id of the server that has sent the message
     */
    public void getSenderID() {
    	senderID = Integer.parseUnsignedInt(header[2]);
    }

    /**
     * Stores the file identifier for the backup service.
     *
     * "It is supposed to be obtained by using the SHA256 cryptographic hash function.
     *
     * As its name indicates its length is 256 bit, i.e. 32 bytes, and should be encoded
     * as a 64 ASCII character sequence.
     *
     * The encoding is as follows: each byte of the
     * hash value is encoded by the two ASCII characters corresponding to the hexadecimal
     * representation of that byte. E.g., a byte with value 0xB2 should be represented by
     * the two char sequence 'B''2' (or 'b''2', it does not matter).
     * The entire hash is represented in big-endian order, i.e. from the MSB (byte 31) to the LSB (byte 0)."
     * @return -1 if error
     */
    public int getFileID() {
    	if(header[3].length() != 64) {
    		System.out.println("The length of this field is 32 bytes and should be encoded as a 64 ASCII char seq.");
    		return -1;
    	}

    	String strFileID = "";

    	for(int i = 0; i < 32; i++) {
    		strFileID += "0x" + Character.toUpperCase(header[3].charAt(i)) + Character.toUpperCase(header[3].charAt(i + 1));
    		i++;
    	}
    	fileName = header[3];
    	fileID = Integer.parseInt(strFileID);
    	return 0;
    }

    /**
     * Stores the chunk number.
     *
     * "ChunkNo together with the FileId specifies a chunk in the file.
     *
     * The length of this field is variable, but should not be larger than 6 chars.
     *
     * Given that each chunk is 64 KByte,
     * this limits the size of the files to backup to 64 GByte."
     * @return -1 if error
     */
    public int getChunkNo() {
    	if(header[4].length() > 6) {
    		System.out.println("The length of this field should not be larger than 6 chars.");
    		return -1;
    	}
    	chunkNo = Integer.parseUnsignedInt(header[4]);
    	return 0;
    }

    /**
     * Stores the replication degree of the chunk.
     *
     * "This is a digit, thus allowing a replication degree of up to 9.
     *
     * It takes one byte, which is the ASCII code of that digit."
     * @return -1 if error
     */
    public int getReplicationDeg() {
    	if(header[5].length() > 1) {
    		System.out.println("The length of this field should be one byte");
    		return -1;
    	}
    	replicationDeg = Integer.parseInt(header[5]);
    	return 0;
    }

    /**
     * Stores the header fields accordingly to the message type
     * @return lower than 0 if error
     */
    public int verifyMessageType() {
    	messageType = header[0];
    	int error = 0;
    	error += getVersion();
    	getSenderID();
    	error += getFileID();

    	if(!messageType.equals("DELETE")) {
    		error += getChunkNo();
    	}

    	if(messageType.equals("PUTCHUNK")) {
    		error += getReplicationDeg();
    	}

    	if(messageType.equals("PUTCHUNK") || messageType.equals("CHUNK")) {
    		body = new byte[stream.available()];
            stream.read(body, 0, body.length);
    	}

    	return error;
    }


}
