/**
 * 
 */
package server;

import java.util.Random;

public class Utils {
	public static final int TIME_MAX_TO_SLEEP = 400;
	public static void randonSleep(int time) throws InterruptedException {
		Random r = new Random();
		Thread.sleep(r.nextInt(time));
	}
}
