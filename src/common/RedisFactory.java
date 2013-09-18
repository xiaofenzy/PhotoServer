package common;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

import redis.clients.jedis.Jedis;


public class RedisFactory {
	
	private String confPath = "conf.txt";
	private String remoteRedisHost;
	private int remoteRedisPort;
	public RedisFactory()
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
					if(ss[0].equals("remoteRedisHost"))
						remoteRedisHost = ss[1];
					if(ss[0].equals("remoteRedisPort"))
						remoteRedisPort = Integer.parseInt(ss[1]);
				}
			}
			br.close();
			
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	//从配置文件中读取redis的地址和端口,以此创建jedis对象
	public Jedis getDefaultInstance()
	{
		return new Jedis(remoteRedisHost,remoteRedisPort);
	}
	public static Jedis getNewInstance(String host,int port)
	{
			Jedis jedis = new Jedis(host,port);
			return jedis;
	}
	public static Jedis getNewInstance1(String host,int port)
	{
			Jedis jedis1 = new Jedis(host,port);
			jedis1.select(1);
			return jedis1;
	}
}
