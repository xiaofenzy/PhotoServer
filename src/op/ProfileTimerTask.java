package op;

import java.util.Date;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.TimerTask;

import common.ServerProfile;

public class ProfileTimerTask extends TimerTask {

	private int period;					//每过period秒统计一次平均值
	private long lastWn = 0;
	private long lastTs = System.currentTimeMillis();
	
	public ProfileTimerTask(int period) {
		super();
		this.period = period;
	}

	@Override
	public void run() {
		// TODO Auto-generated method stub
		long wn = ServerProfile.writtenBytes.longValue()/1024;			//单位转换成KB
		long cur = System.currentTimeMillis();
		
		DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		String s = df.format(new Date());
		//System.out.println(s+":平均写入带宽 "+wn/period+"KB/s");
		System.out.println(s+":平均写入带宽 "+ (wn - lastWn) / ((cur - lastTs) / 1000) +"KB/s");
		if(ServerProfile.readN.longValue() == 0)
			System.out.println("暂无读取请求");
		else
			System.out.println(s+":平均读取延迟 "+ServerProfile.readDelay.intValue()/ServerProfile.readN.intValue()+"ms");
		
		lastWn = wn;
		lastTs = cur;
		System.out.println(ServerProfile.readDelay+"  " + ServerProfile.readN);
//		System.out.println("total:" +ServerProfile.total.longValue());
		ServerProfile.reset();
	}

}
