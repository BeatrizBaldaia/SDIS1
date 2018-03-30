package message;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor;

public class SingletonThreadPoolExecutor {
	private static SingletonThreadPoolExecutor instance = null;
	private ScheduledThreadPoolExecutor thread;
	
	
	protected SingletonThreadPoolExecutor() {
		/* ThreadPoolExecutor(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit, BlockingQueue<Runnable> workQueue) */
		RejectedExecutionHandler handler = new MyRejectedExecutionHandler();
		thread = new ScheduledThreadPoolExecutor(Runtime.getRuntime().availableProcessors(), handler);//TODO: ver arguement

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
	public ScheduledThreadPoolExecutor getThreadPoolExecutor() {
		return thread;
	}
}
