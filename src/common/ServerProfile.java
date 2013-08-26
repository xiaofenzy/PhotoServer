package common;

import java.util.concurrent.atomic.AtomicLong;

public class ServerProfile {
	public static AtomicLong writtenBytes = new AtomicLong(0);			//一共写入的字节数,单位字节
	public static AtomicLong readDelay = new AtomicLong(0);				//总读取延迟，单位毫秒
	public static AtomicLong readN = new AtomicLong(0);					//读取次数
	
	public static AtomicLong total = new AtomicLong(0);
	//保证多个线程时的同步
	public static void addWrite(int n)
	{
		writtenBytes.addAndGet(n);
//		total.addAndGet(n);
	}
	
	//增加一次延迟代表多了一个client来读
	public static void addDelay(long d)
	{
		readDelay.addAndGet(d);
		readN.getAndIncrement();			
		total.getAndIncrement();
	}
	
	public static void reset()
	{
//		writtenBytes.set(0);
		readDelay.set(0);
		readN.set(0);
	}
}
