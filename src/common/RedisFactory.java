package common;

import redis.clients.jedis.Jedis;


public class RedisFactory {
//	private static Jedis jedis = null;		//专门用来操作redis中数据库0,0中里面存储的是md5与存储时的返回的映射
//	private static Jedis jedis1 = null;		//用来操作1,1中存储的是每个集合当前可写的块,set--block＿id
	
	//单例模式,免得每个线程都要连接服务器
	//开始时候想写成单例，可是发现这样好像在并发访问的时候会出问题，就该成每次都是重新获得一个实例
	public static Jedis getNewRemoteInstance(String host,int port)
	{
//		if(jedis == null)
		{
			Jedis jedis = new Jedis(host,port);
			return jedis;
		}
//		return jedis;
	}
	public static Jedis getNewLocalInstance(int port)
	{
//		if(jedis1 == null)
		{
			Jedis jedis1 = new Jedis("localhost",port);
			jedis1.select(1);
			return jedis1;
		}
//		return jedis1;
	}
}
