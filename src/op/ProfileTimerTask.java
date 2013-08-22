package op;

import java.util.Date;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.TimerTask;

import common.ServerProfile;

public class ProfileTimerTask extends TimerTask {

	private int period;					//每过period秒统计一次平均值
	
	public ProfileTimerTask(int period) {
		super();
		this.period = period;
	}

	@Override
	public void run() {
		// TODO Auto-generated method stub
		long wn = ServerProfile.writtenBytes.longValue()/1024;			//单位转换成KB
		
		DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		String s = df.format(new Date());
		System.out.println(s+":平均写入带宽 "+wn/period+"KB/s");
		if(ServerProfile.readN.intValue() == 0)
			System.out.println("暂无读取请求");
		else
			System.out.println(s+":平均读取延迟 "+ServerProfile.readDelay.intValue()/ServerProfile.readN.intValue()+"ms");
		
//		System.out.println("total:" +ServerProfile.total);
//		System.out.println("readDelay:"+ServerProfile.readDelay.intValue());
//		System.out.println("readN:"+ServerProfile.readN.intValue());
		ServerProfile.reset();
	}

}
