package server;

import java.util.Random;

public class Utils {
	public static final int TIME_MAX_TO_SLEEP = 400;
	public static final String ENCODING_TYPE = "ISO-8859-1";
	public static final int MAX_LENGTH_CHUNK = 64000;
	public static final int BYTE_TO_KBYTE = 1000;
	public static void randonSleep(int time) throws InterruptedException {
		Random r = new Random();
		Thread.sleep(r.nextInt(time));
	}
}
