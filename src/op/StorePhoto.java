package op;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.InetAddress;

import redis.clients.jedis.Jedis;

import common.RedisFactory;

public class StorePhoto {
	private String localHostName ;					//本机名称,在我的机器上是zhaoyang-pc,用做node名
	private String destRoot = "photo/";
	private String confPath = "conf.txt";			//配置文件
	private String remoteRedisHost;							//远程redis服务器地址
	private int remoteRedisPort;							//端口号
	private int localRedisPort;								//本地redis服务端口
	private int dirnum;								//子目录层数	
	private long blocksize;							//文件块的大小，单位是B
	private String curBlock = null;					//当前可写的块
	private Jedis jedis,jedis1;
	
	public StorePhoto()
	{
		try {
			BufferedReader br = new BufferedReader(new FileReader(confPath));
			String line = "";
			while(true)
			{
				line = br.readLine();
				if(line == null)
					break;
				else if(!line.startsWith("#"))		
				{
					String[] ss = line.split("=");
					if(ss[0].equals("remoteRedisHost"))
						remoteRedisHost = ss[1];
					if(ss[0].equals("remoteRedisPort"))
						remoteRedisPort = Integer.parseInt(ss[1]);
					if(ss[0].equals("localRedisPort"))
						localRedisPort = Integer.parseInt(ss[1]);
					if(ss[0].equals("dirnum"))
						dirnum = Integer.parseInt(ss[1]);
					if(ss[0].equals("blocksize"))
						blocksize = Long.parseLong(ss[1])*1024*1024;		//配置文件中单位是MB,在这里转换B
					
				}
			}
//			jedis = new Jedis(host,port);
			jedis = RedisFactory.getNewRemoteInstance(remoteRedisHost, remoteRedisPort);
			jedis1 = RedisFactory.getNewLocalInstance(localRedisPort);
			br.close();
			
			localHostName = InetAddress.getLocalHost().getHostName();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
	/**
	 * 把content代表的图片内容,存储起来,把小图片合并成一个块,块大小由配置文件中blocksize指定.
	 * 文件存储在destRoot下，然后按照set分第一层子目录,按照参数md5再分dirnum层子目录
	 * @param set	集合名
	 * @param md5	文件的md5
	 * @param content	文件内容
	 * @return		type#set#node#＃path＃offset＃length,这几个信息通过redis的哈希表存储,分别表示该图片所属集合,所在相对路径（包括完整文件名），
	 * 				该图片的字节数，和位于所在块的偏移的字节数
	 */
	public String storePhoto(String set, String md5, byte[] content)
	{
		long length,offset = 0;			//用于构造返回值
		String location;
		length = content.length;
		
		//根据set和md5构造存储的路径
		StringBuffer sb = new StringBuffer();
		sb.append(destRoot);
		sb.append(set);
		sb.append("/");
		for(int i = 0;i<dirnum;i++)
		{
			sb.append(md5.charAt(i));
			sb.append("/");
		}
		String path = sb.toString();		//存储文件的文件夹的相对路径，不包含文件名
		File dir = new File(path);			
		if (!dir.exists())
			dir.mkdirs();
		//找到当前可写的文件块,如果当前不够大,或不存在,则新创建一个,命名block＿id,id递增
		//用curBlock缓存当前可写的块，减少查询jedis的次数
		File newf;
		if(curBlock == null)
		{
			if(jedis.get(set) != null)
			{
				curBlock = jedis.get(set);
				newf = new File(path+curBlock);
			}
			else
			{
				curBlock = "block_1";
				newf = new File(path+curBlock);
				jedis1.set(set, curBlock);
			}
			
		}
		else
		{
			newf = new File(path+curBlock);
			if(newf.length()+content.length > blocksize)
			{
				curBlock ="block_"+ (Integer.parseInt(curBlock.split("_")[1]) + 1);
				newf = new File(path+curBlock);
				jedis1.set(set, curBlock);
			}
		}
		
		location = path+newf.getName();				//文件块的完整路径名，包括文件名
			StringBuffer rVal = new StringBuffer();
			try {
				
				RandomAccessFile raf = new RandomAccessFile(newf, "rw");
				offset = raf.length();
				raf.seek(offset);
				raf.write(content);
				raf.close();
				//构造返回值
				rVal.append("1#");			//type
				rVal.append(set);
				rVal.append("#");
				rVal.append(localHostName);			//node
				rVal.append("#");
				rVal.append(location);
				rVal.append("#");
				rVal.append(offset);
				rVal.append("#");
				rVal.append(length);
//				rVal.append("#");
//				rVal.append(1);				//ref
//				rVal.append("#");
//				rVal.append(1);				//exist

			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			/*
			Map<String,String> hash = new Hashtable<String,String>();
			hash.put("type", "1");
			hash.put("set",set);
			hash.put("node", localHostName);
			hash.put("location", location);
			hash.put("offset", offset+"");
			hash.put("length", length+"");
			hash.put("ref", "1");
			hash.put("exist", "1");
			jedis.hmset(md5, hash);
			*/
			String returnVal = rVal.toString();
			//确保多个进程生成的字符串只有一个被记录下来并且被完整的记录下
			if(jedis.setnx(md5, returnVal) == 1)
			{
				jedis.incr(md5+".ref");
				return returnVal;
			}
			else
			{
				jedis.incr(md5+".ref");				//增加引用计数
				return jedis.get(md5);
			}
		
		
	}
	/**
	 * 存储多个图片，通过反复调用storePhoto实现
	 * @param set
	 * @param md5
	 * @param content
	 * @return
	 */
	public String[] mstorePhoto(String[] set,String[] md5,byte[][] content)
	{
		if(set.length == md5.length && md5.length == content.length)
		{
			int length = set.length;
			String[] infos = new String[length];
			for(int i = 0;i<length;i++)
			{
				infos[i] = storePhoto(set[i],md5[i],content[i]);
				
			}
			return infos;
		}
		else
		{
			System.out.println("数组长度不匹配");
			return null;
		}
	}
	/**
	 *获得md5值所代表的图片的内容
	 * @param md5		与storePhoto中的参数md5相对应
	 * @return			该图片的内容,与storePhoto中的参数content对应
	 */
	public byte[] getPhoto(String md5)
	{
		if(jedis.get(md5) == null)
		{
			System.out.println("图片不存在:"+md5);
			return null;
		}
		String info = jedis.get(md5);
		return searchPhoto(info);
	}
	/**
	 * 获得图片内容
	 * @param info		对应storePhoto的type#set#node#＃path＃offset＃length格式的返回值
	 * @return			图片内容content
	 */
	public byte[] searchPhoto(String info)
	{
		String[] infos = info.split("#");			
		byte[] content = new byte[Integer.parseInt(infos[5])];
		try {
			RandomAccessFile raf = new RandomAccessFile(infos[3], "r");
			raf.seek(Long.parseLong(infos[4]));
			raf.read(content);
			raf.close();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		} catch (NumberFormatException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}
		return content;
	}
	
	@Override
	protected void finalize()		//在该类被回收时关闭jedis连接
	{
		try {
			super.finalize();
		} catch (Throwable e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		jedis.quit();
		jedis1.quit();
	}
	
}
