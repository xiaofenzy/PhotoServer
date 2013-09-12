package zy;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.net.SocketException;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import common.ActionType;
import common.ServerProfile;
import common.WriteTask;
import op.StorePhoto;
import op.WriteThread;

public class Handler implements Runnable{
	private Hashtable<String,BlockingQueue<WriteTask>> sq;
	private Socket s;
	
	private StorePhoto sp;
	
	private DataInputStream dis;
	private DataOutputStream dos;					//向客户端的输出流
	public Handler(Socket s,Hashtable<String,BlockingQueue<WriteTask>> sq)
	{
//		System.out.println(s.getRemoteSocketAddress()+"kaishi");
		this.s = s;
		this.sq = sq;
		try {
			dis = new DataInputStream(this.s.getInputStream());
			dos = new DataOutputStream(this.s.getOutputStream());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	@Override
	public void run() {
		// TODO Auto-generated method stub
		try {
			while(true)
			{
				byte[] header = new byte[4];
//				long cu = System.currentTimeMillis();		//记录这个read执行时间
				if((dis.read(header)) == -1)
				{
					break;
				}
				else if(header[0] == ActionType.STORE)
				{
//					System.out.println("in store "+(System.currentTimeMillis()-cu));		//1,2,4,5ms
					int setlen = header[1];
					int md5len = header[2];
					int contentlen = dis.readInt();
					
					byte[] setmd5content = readBytes(setlen+md5len+contentlen, dis);	//一次把所有的都读出来,减少读取次数	
					String set = new String(setmd5content,0,setlen);
					String md5 = new String(setmd5content,setlen,md5len);
//					byte[] content = Arrays.copyOfRange(setmd5content, setlen+md5len, setlen+md5len+contentlen);
					
					ServerProfile.addWrite(contentlen);			//统计写入的字节数
					
					WriteTask t = new WriteTask(set,md5, setmd5content, setlen + md5len, contentlen);
					synchronized (t) {
						if(sq.containsKey(set))				//存在这个键,表明该写线程已经存在,直接把任务加到任务队列里即可
						{
							sq.get(set).add(t);
							try {
								t.wait();
							} catch (InterruptedException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
						}
						else				//如果不存在这个键,则需要新开启一个写线程
						{
							BlockingQueue<WriteTask> tasks = new LinkedBlockingQueue<WriteTask>();
							tasks.add(t);
							sq.put(set, tasks);
							WriteThread wt = new WriteThread(tasks);
							new Thread(wt).start();
							try {
								t.wait();
							} catch (InterruptedException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
						}
					}
					
					//把写的结果返回给客户端
					if(t.getResult().equals("#"))		//说明结果已经在redis里存在
						dos.writeInt(-1);
					else
					{
						dos.writeInt(t.getResult().getBytes().length);
						dos.write(t.getResult().getBytes());
					}
					dos.flush();
				}
				
				else if(header[0] == ActionType.SEARCH)
				{
					s.setTcpNoDelay(true);
//					System.out.println("in search "+(System.currentTimeMillis()-cu));		//前2次请求时间很短,1,2ms,后面都是40,38ms
//					System.out.println("in search");
					long start = System.currentTimeMillis();
					int infolen = header[1];
//					long s = System.currentTimeMillis();
					String info = new String(readBytes(infolen,dis));			//每次都有39 40ms延迟
//					System.out.println(System.currentTimeMillis()-s);
					if(sp == null)			//有读请求时,才初始化该对象
						sp = new StorePhoto();
					byte[] content = sp.searchPhoto(info);
					if(content == null)			//有可能刚刚写进redis的时候，还无法马上读出来,这时候会无法找到图片,返回null
					{
						dos.writeInt(0);
						dos.flush();
					}
					else{
						dos.writeInt(content.length);
						dos.write(content);
						dos.flush();
					}
					ServerProfile.addDelay(System.currentTimeMillis() - start);
//					System.out.println("in search end");
				}
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
//		System.out.println(s.getRemoteSocketAddress()+"jieshu");
	}
	
	
	/**
	 * 从输入流中读取count个字节
	 * @param count
	 * @return
	 */
	public byte[] readBytes(int count,InputStream istream)
	{
		byte[] buf = new byte[count];			
		int n = 0;
		try {
			while(count > n)
			{
				n += istream.read(buf,n,count-n);
			}
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return buf;
	}
}
