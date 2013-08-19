package test;

import java.io.File;

import zy.PhotoServer;

public class STest {

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		PhotoServer ps = new PhotoServer();
		ps.startUp();
//		Jedis j = new Jedis("127.0.0.1",6379);
//		j.select(0);
//		System.out.println(j.get("004.gif"));
//		File dir = new File("photo/");
//		for(File f : dir.listFiles())
//			if(f.isDirectory())
//			System.out.println(f.getName());
	}

}
