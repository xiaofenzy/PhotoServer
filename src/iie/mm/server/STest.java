package iie.mm.server;



public class STest {

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		if(args.length>0)
			ServerConf.setNodeName(args[0]);
		PhotoServer ps = new PhotoServer();
		ps.startUp();
	}

}
