package zy;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Hashtable;
import java.util.Timer;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.newsclub.net.unix.AFUNIXServerSocket;
import org.newsclub.net.unix.AFUNIXSocketAddress;

import op.ProfileTimerTask;
import common.WriteTask;

public class PhotoServer {
	private ServerSocket ss;
	private int serverport;
	private String confPath = "conf.txt";			//配置文件
	private int period;								//每隔period秒统计一次读写信息
//	private String destRoot = "photo/";
	private ExecutorService pool;
	//集合跟到这个集合上的写操作队列的映射
	private Hashtable<String,BlockingQueue<WriteTask>> sq = new Hashtable<String, BlockingQueue<WriteTask>>();		
	public PhotoServer()
	{
		try {
			BufferedReader br = new BufferedReader(new FileReader(confPath));
			String line = "";
			while(true)
			{
				line = br.readLine();
				if(line == null)
					break;
				else if(!line.startsWith("#"))		
				{
					String[] ss = line.split("=");
					if(ss[0].equals("serverport"))
						serverport = Integer.parseInt(ss[1]);
					if(ss[0].equals("period"))
						period = Integer.parseInt(ss[1]);
				}
			}
			br.close();
			
			ss = new ServerSocket(serverport);
			pool = Executors.newCachedThreadPool();
			
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void startUp()
	{
		//服务端每隔一段时间进行一次读写速率统计,10秒后开始统计，每10秒输出一次平均信息
		Timer t = new Timer();
		t.schedule(new ProfileTimerTask(period),10*1000, period*1000);
		
		//启动监听写请求的服务,它使用junixsocket,所以需要用一个新的线程
		new Thread(new WriteServer()).start();
		
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

