package ftp.server;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * 
 * @author vincentlee
 *
 */

public class FTPServer {
	private Map<String, String> transferMap;
	
	
	
	public FTPServer() {
		transferMap = new HashMap<String, String>();
	}
	
	public synchronized void terminate(String uuid) {
		
	}
	
	public synchronized void getIN() {
		ReadWriteLock lock = new ReentrantReadWriteLock();
		lock.writeLock().lock();
	}
}
