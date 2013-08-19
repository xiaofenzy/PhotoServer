package zy;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.net.ServerSocket;
import java.util.Hashtable;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import common.WriteTask;

public class PhotoServer {
	private ServerSocket ss;
	private int serverport;
	private String confPath = "conf.txt";			//配置文件
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
		while(true)
		{
			try {
				pool.execute(new Handler(ss.accept(),sq));
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				pool.shutdown();
			}
		}
	}
}
