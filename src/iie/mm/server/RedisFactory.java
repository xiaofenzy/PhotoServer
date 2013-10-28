package iie.mm.server;

import redis.clients.jedis.Jedis;

public class RedisFactory {

	private String remoteRedisHost;
	private int remoteRedisPort;

	public RedisFactory() {
		remoteRedisHost = ServerConf.getRedisHost();
		remoteRedisPort = ServerConf.getRedisPort();
	}

	// 从配置文件中读取redis的地址和端口,以此创建jedis对象
	public Jedis getDefaultInstance() {
		return new Jedis(remoteRedisHost, remoteRedisPort);
	}

	public static Jedis getNewInstance(String host, int port) {
		Jedis jedis = new Jedis(host, port);
		return jedis;
	}

	public static Jedis getNewInstance1(String host, int port) {
		Jedis jedis1 = new Jedis(host, port);
		jedis1.select(1);
		return jedis1;
	}
}
