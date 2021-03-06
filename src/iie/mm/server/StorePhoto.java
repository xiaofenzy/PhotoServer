package iie.mm.server;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.Pipeline;

public class StorePhoto {
	private String localHostName ;					//本机名称,在我的机器上是zhaoyang-pc,用做node名
	private int serverport;							//本机监听的端口,在这里的作用就是构造存图片时返回值
	private String destRoot = "photo/";
//	private String confPath = "conf.txt";			//配置文件
	private String remoteRedisHost;							//远程redis服务器地址
	private int remoteRedisPort;							//端口号
//	private int dirnum;								//子目录层数	
	private long blocksize;							//文件块的大小，单位是B
	private long curBlock = -1;					//当前可写的块
	private RandomAccessFile raf = null;					//写当前的块的随机访问流
	private File newf;			//代表要写的块的文件
	private Hashtable<String,RandomAccessFile> readRafHash;			//读文件时的随机访问流，用哈希来缓存
	private Jedis jedis,jedis1;
	private long offset = 0;					//写文件时,记录偏移
	
	public StorePhoto() {
		remoteRedisHost = ServerConf.getRedisHost();
		remoteRedisPort = ServerConf.getRedisPort();
		serverport = ServerConf.getServerPort();
		blocksize = ServerConf.getBlockSize() * 1024 * 1024; // 配置文件中单位是MB,在这里转换B

		jedis = RedisFactory.getNewInstance(remoteRedisHost, remoteRedisPort);
		jedis1 = RedisFactory.getNewInstance1(remoteRedisHost, remoteRedisPort);

		localHostName = ServerConf.getNodeName();
		readRafHash = new Hashtable<String, RandomAccessFile>();
	}

	/**
	 * 把content代表的图片内容,存储起来,把小图片合并成一个块,块大小由配置文件中blocksize指定.
	 * 文件存储在destRoot下，然后按照set分第一层子目录
	 * @param set	集合名
	 * @param md5	文件的md5
	 * @param content	文件内容
	 * @return		type#set#node#port＃path＃offset＃length,这几个信息通过redis存储,分别表示元信息类型,该图片所属集合,所在节点,
	 * 				节点的端口号,所在相对路径（包括完整文件名）,位于所在块的偏移的字节数，该图片的字节数
	 */
	public String storePhoto(String set, String md5, byte[] content, int coff, int clen)
	{
		long length = clen;			//用于构造返回值
		String location;
		
		//根据set和md5构造存储的路径
		StringBuffer sb = new StringBuffer();
		sb.append(destRoot);
		sb.append(set);
		sb.append("/");
		String path = sb.toString();		//存储文件的文件夹的相对路径，不包含文件名

		//找到当前可写的文件块,如果当前不够大,或不存在,则新创建一个,命名block＿id,id递增,redis中只存储id
		//用curBlock缓存当前可写的块，减少查询jedis的次数
		
		File dir;			//要写的块所在文件夹
		StringBuffer rVal = new StringBuffer();
		try 
		{
			if (curBlock < 0) 
			{
				String reply = jedis1.get(set + "." + localHostName);
				if (reply != null) {
					curBlock = Long.parseLong(reply);		//需要通过节点名字来标示不同节点上相同名字的集合
					newf = new File(path + "b" + curBlock);
				} 
				else 
				{
					curBlock = 1;
					dir = new File(path);
					if(!dir.exists())
						dir.mkdirs();
					newf = new File(path + "b" + curBlock);
					jedis1.sadd(set+".l", localHostName+"#"+serverport);			//把集合和它所在节点记录在redis的set里,方便删除,set.l表示set所在的位置
					jedis1.set(set + "." + localHostName, "" + curBlock);
				}
				raf = new RandomAccessFile(newf, "rw");
				offset = newf.length();
				raf.seek(offset);
			} 
			else 
			{
				if (newf.length() + content.length > blocksize) 
				{
					curBlock++;
					newf = new File(path + "b" + curBlock);
					// jedis1.set(set, curBlock);
					if(raf != null)			//如果换了一个新块,则先把之前的关掉
						raf.close();
					raf = new RandomAccessFile(newf, "rw");
					jedis1.incr(set + "." + localHostName);			//当前可写的块号加一
					offset = 0;
				}
			}
			// 文件块的路径名，包括文件名,不包括destRoot指定的第一级目录,解析时需要加上
			location = path.substring(destRoot.length()) + newf.getName();

			raf.write(content,coff,clen);
			// raf.close();
			// 构造返回值
			rVal.append("1#"); // type
			rVal.append(set);
			rVal.append("#");
			rVal.append(localHostName); // node
			rVal.append("#");
			rVal.append(serverport); // port
			rVal.append("#");
			rVal.append(location);
			rVal.append("#");
			rVal.append(offset);
			rVal.append("#");
			rVal.append(length);

			offset += length;
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		String returnVal = rVal.toString();
		// 确保多个进程生成的字符串只有一个被记录下来并且被完整的记录下
		Pipeline pipeline = jedis.pipelined();
//		pipeline.incr(set+".r." + md5);
//		pipeline.setnx(set+"."+md5, returnVal);
		pipeline.hincrBy(set, "r."+md5, 1);
		pipeline.hsetnx(set, md5, returnVal);
		List<Object> l = pipeline.syncAndReturnAll();
//		System.out.println(l.get(1).getClass());
		if((Long)l.get(1) == 1)
			return returnVal;
		else
			return "#";
		

	}
	/**
	 * 存储多个图片，通过反复调用storePhoto实现
	 * @param set
	 * @param md5
	 * @param content
	 * @return
	 */
	public String[] mstorePhoto(String[] set,String[] md5,byte[][] content, int[] coff, int[] clen)
	{
		if(set.length == md5.length && md5.length == content.length)
		{
			int length = set.length;
			String[] infos = new String[length];
			for(int i = 0;i<length;i++)
			{
				infos[i] = storePhoto(set[i],md5[i],content[i], coff[i], clen[i]);
				
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
	public byte[] getPhoto(String set,String md5)
	{
		String info = jedis.hget(set, md5);
		if(info == null)
		{
			System.out.println("图片不存在:"+set+"."+md5);
			
			return null;
		}
		return searchPhoto(info);
	}
	/**
	 * 获得图片内容
	 * @param info		对应storePhoto的type#set#node#port#path＃offset＃length格式的返回值
	 * @return			图片内容content
	 */
	public byte[] searchPhoto(String info)
	{
		String[] infos = info.split("#");		
		String path = infos[4];
		RandomAccessFile readr = null;
		byte[] content = new byte[Integer.parseInt(infos[6])];
		try {
			//用哈希缓存打开的文件随机访问流
			if(readRafHash.containsKey(path))
			{
				readr = readRafHash.get(path);
			}
			else
			{
				readr = new RandomAccessFile(destRoot+path, "r");
				readRafHash.put(path, readr);
			}
			readr.seek(Long.parseLong(infos[5]));
			readr.read(content);
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
	
	public void delSet(String set)
	{
		delFile(new File(destRoot+set));
	}
	/**
	 * 删除文件.如果是一个文件,直接删除,如果是文件夹,递归删除子文件夹和文件
	 * @param f
	 */
	private void delFile(File f)
	{
		
		if(!f.exists())
			return;
		if(f.isFile())
			f.delete();
		else 
		{
			for (File a : f.listFiles())
				if (a.isFile())
					a.delete();
				else
					delFile(a);
		}
		f.delete();
	}
	
	public void close()		//关闭jedis连接,关闭文件访问流
	{
		try {
			if(raf != null)
				raf.close();
			Enumeration<RandomAccessFile> er = readRafHash.elements();
			while(er.hasMoreElements())
				er.nextElement().close();
		} 
		catch(IOException e){
			e.printStackTrace();
		}
		jedis.quit();
		jedis1.quit();
		
	}
	
}
