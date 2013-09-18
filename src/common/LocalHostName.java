package common;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * 获得本机名称,得到自动获得的值,或者得到setname设定的值
 * @author zhaoyang
 *
 */
public class LocalHostName {
	private static String name ;
	public static String getName() {
		return name;
	}
	public static void setName(String name) {
		LocalHostName.name = name;
	}
	static{
		try {
			name = InetAddress.getLocalHost().getHostName();
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 
	}
	
	
	
}
