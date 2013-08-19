package op;

import java.util.concurrent.BlockingQueue;

import common.WriteTask;

public class WriteThread implements Runnable {

//	private String set;
	private BlockingQueue<WriteTask> tasks;
	
	
	public WriteThread( BlockingQueue<WriteTask> tasks) {
//		this.set = set;
		this.tasks = tasks;
	}


	@Override
	public void run() {
		// TODO Auto-generated method stub
		StorePhoto sp = new StorePhoto();
		try {
			while(true)
			{

				WriteTask t = tasks.take();
				synchronized (t) {
					t.setResult(sp.storePhoto(t.getSet(), t.getMd5(), t.getContent()));
					t.notify();
				}
				
			}
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
