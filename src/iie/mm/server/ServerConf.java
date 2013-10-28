package iie.mm.server;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.Pipeline;

/**
 * 代表节点的配置
 * 
 * @author zhaoyang
 * 
 */
public class ServerConf {
	private static String nodeName; // 节点名
	private static int serverPort;
	private static String redisHost;
	private static int redisPort;
	private static int blockSize;
	private static int period; // 每隔period秒统计一次读写速率

	static {
		//从配置文件获取配置
		try {
			nodeName = InetAddress.getLocalHost().getHostName();
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		try {
			BufferedReader br = new BufferedReader(new FileReader("conf.txt"));
			String line = "";
			while (true) {
				line = br.readLine();
				if (line == null)
					break;
				else if (!line.startsWith("#")) {
					String[] ss = line.split("=");
					if (ss[0].equals("redisHost"))
						redisHost = ss[1];
					if (ss[0].equals("redisPort"))
						redisPort = Integer.parseInt(ss[1]);
					if (ss[0].equals("serverport"))
						serverPort = Integer.parseInt(ss[1]);
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
		
		//从redis获取配置
		Jedis jedis = new RedisFactory().getDefaultInstance();
		Pipeline p = jedis.pipelined();
		p.select(1);
		p.get("conf.blocksize");
		p.get("conf.period");
		List<Object> results = p.syncAndReturnAll();
		blockSize = Integer.parseInt(results.get(1).toString());
		period = Integer.parseInt(results.get(2).toString());
//		System.out.println(period);
	}


	public static String getNodeName() {
		return nodeName;
	}

	public static void setNodeName(String name) {
		ServerConf.nodeName = name;
	}

	public static int getServerPort() {
		return serverPort;
	}

	public static void setServerPort(int serverPort) {
		ServerConf.serverPort = serverPort;
	}

	public static String getRedisHost() {
		return redisHost;
	}

	public static void setRedisHost(String redisHost) {
		ServerConf.redisHost = redisHost;
	}

	public static int getRedisPort() {
		return redisPort;
	}

	public static void setRedisPort(int redisPort) {
		ServerConf.redisPort = redisPort;
	}

	public static int getBlockSize() {
		return blockSize;
	}

	public static void setBlockSize(int blockSize) {
		ServerConf.blockSize = blockSize;
	}

	public static int getPeriod() {
		return period;
	}

	public static void setPeriod(int period) {
		ServerConf.period = period;
	}

}
