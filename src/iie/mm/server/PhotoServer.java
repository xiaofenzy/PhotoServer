package iie.mm.server;

import java.io.File;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Timer;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.newsclub.net.unix.AFUNIXServerSocket;
import org.newsclub.net.unix.AFUNIXSocketAddress;

public class PhotoServer {
	private ServerSocket ss;
	private int serverport;
	private int period ;								//每隔period秒统计一次读写信息
//	private String destRoot = "photo/";
	private ExecutorService pool;
	//集合跟到这个集合上的写操作队列的映射
	private ConcurrentHashMap<String,BlockingQueue<WriteTask>> sq = new ConcurrentHashMap<String, BlockingQueue<WriteTask>>();		
	public PhotoServer()
	{
			serverport = ServerConf.getServerPort();
			period = ServerConf.getPeriod();
			try {
				ss = new ServerSocket(serverport);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			pool = Executors.newCachedThreadPool();
			
	}
	
	public void startUp()
	{
		//服务端每隔一段时间进行一次读写速率统计,1秒后开始统计，每10秒输出一次平均信息
		Timer t = new Timer();
		t.schedule(new ProfileTimerTask(period),1*1000, period*1000);
		
		//启动监听写请求的服务,它使用junixsocket,所以需要用一个新的线程
//		new Thread(new WriteServer()).start();
		
		while(true)
		{
			try {
				//接收tcp请求,来自tcp的请求一定都是读取请求
				pool.execute(new Handler(ss.accept(),sq));
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				pool.shutdown();
			}
		}
	}
	
	
	
	/**
	 * 专门用来接收写请求，使用junixsocket,应该可以实现并tcp更快的进程间通信
	 * @author zhaoyang
	 *
	 */
	class WriteServer implements Runnable
	{

		@Override
		public void run() {
			//在本机部署多个服务端,要修改
			final File socketFile = new File(new File(System.getProperty("java.io.tmpdir")), "junixsocket-test.sock");		
			ExecutorService pool = Executors.newCachedThreadPool(); 
			AFUNIXServerSocket server;
			 
				try {
					server = AFUNIXServerSocket.newInstance();
					server.bind(new AFUNIXSocketAddress(socketFile));
//				    System.out.println("server: " + server.getInetAddress());
				    while (true) {
			            Socket sock = server.accept();
			            pool.execute((new Handler(sock,sq)));
				    }
				} catch (IOException e) {
					// TODO Auto-generated catch block
					pool.shutdown();
					e.printStackTrace();
				}
		       
		}
		
	}
}

