package zy;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.Hashtable;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import common.ServerProfile;
import common.WriteTask;
import op.StorePhoto;
import op.WriteThread;

public class Handler implements Runnable{
	private Hashtable<String,BlockingQueue<WriteTask>> sq;
	private Socket s;
	
	private InputStream is;
//	private BufferedReader isBr;				//从客户端的输入流,用BufferedReader把is封装起来
	private OutputStream os;					//向客户端的输出流
	public Handler(Socket s,Hashtable<String,BlockingQueue<WriteTask>> sq)
	{
//		System.out.println(s.getRemoteSocketAddress()+"kaishi");
		this.s = s;
		this.sq = sq;
		try {
			is = this.s.getInputStream();
//			isBr = new BufferedReader(new InputStreamReader(is));
			os = this.s.getOutputStream();
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
				
				String action = readline();
				
//				System.out.println("action"+action);
				if(action == null)
				{
					break;
				}
				else if(action.equals("store"))
				{
					String set = readline();
					String md5 = readline();
					int n = Integer.parseInt(readline());
					ServerProfile.addWrite(n);			//统计写入的字节数
					byte[] content = readBytes(n);
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
					os.write((t.getResult().getBytes().length+"\n").getBytes());
					os.write(t.getResult().getBytes());
					os.flush();
				}
				else if(action.equals("get"))
				{
					long start = System.currentTimeMillis();			//统计读取延迟
					String md5 = readline();
					StorePhoto sp = new StorePhoto();
					byte[] content = sp.getPhoto(md5);
					if(content == null)			//有可能刚刚写进redis的时候，还无法马上读出来,这时候会无法找到图片,返回null
					{
						os.write("0\n".getBytes());
						os.flush();
					}
					else{
						os.write((content.length+"\n").getBytes());
						os.write(content);
						os.flush();
					}
					ServerProfile.addDelay(System.currentTimeMillis() - start);
				}
				else if(action.equals("search"))
				{
					long start = System.currentTimeMillis();
					String info = readline();
					StorePhoto sp = new StorePhoto();
					byte[] content = sp.searchPhoto(info);
					if(content == null)			//有可能刚刚写进redis的时候，还无法马上读出来,这时候会无法找到图片,返回null
					{
						os.write("0\n".getBytes());
						os.flush();
					}
					else{
						os.write((content.length+"\n").getBytes());
						os.write(content);
						os.flush();
					}
					ServerProfile.addDelay(System.currentTimeMillis() - start);
				}
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
//		System.out.println(s.getRemoteSocketAddress()+"jieshu");
	}
	
	/**
	 * 从输入流读取一行字符串
	 * @return
	 */
	private String readline()
	{
		//这个类就跟StringBuffer作用类似，可以动态的扩展字节数组的大小
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		
		try {
			byte[] b = new byte[1];
			while(-1 != (is.read(b)))
			{
				if(b[0] == '\n')
					break;
				else 
					baos.write(b);
			}
			if(baos.size() == 0)
				return null;
			return new String(baos.toByteArray());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;			//是返回null还是""呢．．
		}
	}
	
	/**
	 * 从输入流中读取count个字节
	 * @param count
	 * @return
	 */
	public byte[] readBytes(int count)
	{
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		byte[] buf = new byte[1024];
		int n;
		try {
			while(count > buf.length)
			{
				n = is.read(buf);
				baos.write(buf, 0, n);
				count -= n;
			}
			
			if(count>0)
			{
				buf = new byte[count];
				is.read(buf);
				baos.write(buf);
			}
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return baos.toByteArray();
	}
}
