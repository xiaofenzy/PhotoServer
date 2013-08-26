package zy;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
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
				
//				System.out.println("action"+action);
				if((dis.read(header)) == -1)
				{
					break;
				}
				else if(header[0] == ActionType.STORE)
				{
					int setlen = header[1];
					int md5len = header[2];
					int contentlen = dis.readInt();
					String set = new String(readBytes(setlen,dis));
					String md5 = new String(readBytes(md5len,dis));
					
					ServerProfile.addWrite(contentlen);			//统计写入的字节数
					byte[] content = readBytes(contentlen,dis);
//					is.skip(is.available());
					WriteTask t = new WriteTask(set,md5,content);
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
					dos.writeInt(t.getResult().getBytes().length);
					dos.write(t.getResult().getBytes());
					dos.flush();
				}
				
				else if(header[0] == ActionType.SEARCH)
				{
//					System.out.println("in search");
					long start = System.currentTimeMillis();
					int infolen = header[1];
					String info = new String(readBytes(infolen,dis));
					if(sp == null)
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
