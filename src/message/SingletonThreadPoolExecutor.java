package message;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class SingletonThreadPoolExecutor {
	private static SingletonThreadPoolExecutor instance = null;
	private ThreadPoolExecutor thread;
	
	
	protected SingletonThreadPoolExecutor() {
		/* ThreadPoolExecutor(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit, BlockingQueue<Runnable> workQueue) */
		BlockingQueue<Runnable> workQueue = new ArrayBlockingQueue<>(10);
		thread = new ThreadPoolExecutor(10, 20, 10L, TimeUnit.MILLISECONDS, workQueue);
		//TODO ver threads
	}

	public static SingletonThreadPoolExecutor getInstance() {
		if(instance == null) {
			instance = new SingletonThreadPoolExecutor();
		}
		return instance;
	}
	
	/**
	 * @return the thread
	 */
	public ThreadPoolExecutor getThreadPoolExecutor() {
		return thread;
	}
}
