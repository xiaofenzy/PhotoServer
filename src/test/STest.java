package test;


import common.LocalHostName;

import zy.PhotoServer;

public class STest {

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		if(args.length>0)
			LocalHostName.setName(args[0]);
		PhotoServer ps = new PhotoServer();
		ps.startUp();
	}

}
