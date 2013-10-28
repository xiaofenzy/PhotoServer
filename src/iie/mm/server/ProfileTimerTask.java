package iie.mm.server;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimerTask;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.Pipeline;

public class ProfileTimerTask extends TimerTask {

	private int period;					//每过period秒统计一次平均值
	private long lastWn = 0;
	private long lastTs = System.currentTimeMillis();
	private String profileDir = "log/";
	private Jedis jedis;
	private String hbkey;
	public ProfileTimerTask(int period) {
		super();
		this.period = period;
		File dir = new File(profileDir);
		if(!dir.exists())
			dir.mkdirs();
		//向redis的数据库1中插入心跳信息
		jedis = new RedisFactory().getDefaultInstance();
		hbkey = "hb."+ServerConf.getNodeName();
		Pipeline pi = jedis.pipelined();
		pi.select(1);
		pi.set(hbkey, "1");
		pi.expire(hbkey, 20);
		pi.set("origin."+ServerConf.getNodeName(), "1");			//启动过的server
		pi.sync();
	}

	@Override
	public void run() {
		// TODO Auto-generated method stub
		long wn = ServerProfile.writtenBytes.longValue()/1024;			//单位转换成KB
		long cur = System.currentTimeMillis();
		DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		String s = df.format(new Date());
		//System.out.println(s+":平均写入带宽 "+wn/period+"KB/s");
		String bandwidth = s+",平均写入带宽 "+ (wn - lastWn) / ((cur - lastTs) / 1000) +"KB/s";
		String readDelay = "";
		if(ServerProfile.readN.intValue() == 0)
			readDelay = ",暂无读取请求";
		else
			readDelay = ",平均读取延迟 "+ServerProfile.readDelay.intValue()/ServerProfile.readN.intValue()+"ms";
		System.out.println(bandwidth+readDelay);
		
		lastWn = wn;
		lastTs = cur;
		
		ServerProfile.reset();
//		System.out.println(ServerProfile.readDelay+"  " + ServerProfile.readN);
//		System.out.println("total:" +ServerProfile.total.longValue());
		
		//server的心跳信息
		jedis.expire(hbkey, 20);
		//把统计信息写入文件,每一天的信息放在一个文件里
		String profileName = s.substring(0, 10)+".txt";
		PrintWriter pw = null;
		try {
			pw = new PrintWriter(new FileOutputStream(profileDir+profileName,true));		//追加到文件尾
			pw.print(bandwidth);
			pw.println(readDelay);
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}finally{
			if(pw != null)
				pw.close();
		}
		
	}

}
