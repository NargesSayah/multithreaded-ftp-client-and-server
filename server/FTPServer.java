package ftp.server;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.Random;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * 
 * @author vincentlee
 *
 */

public class FTPServer {
	private Map<Path, ReentrantReadWriteLock> transferMap;
	private Map<Integer, Path> commandIDMap;
	private Queue<Integer> writeQueue;
	
	public FTPServer() {
		transferMap = new HashMap<Path, ReentrantReadWriteLock>();
		commandIDMap = new HashMap<Integer, Path>();
		writeQueue = new LinkedList<Integer>();
	}
	
	public synchronized int getIN(Path path) {
		int commandID = 0;
		
		//if Path is in transferMap
		if (transferMap.containsKey(path)) {
			//try to get read lock
			if (transferMap.get(path).readLock().tryLock()) {
				//generate unique 5 digit number
				while (commandIDMap.containsKey(commandID = generateID()));
				
				//add to commandIDMap
				commandIDMap.put(commandID, path);
				
				return commandID;
			}
			//didn't get lock
			else
				return -1;
		}
		//acquire lock
		else {
			//add to transferMap and get readLock
			transferMap.put(path, new ReentrantReadWriteLock());
			transferMap.get(path).readLock().lock();
			
			//generate unique 5 digit number
			while (commandIDMap.containsKey(commandID = generateID()));
			
			//add to commandIDMap
			commandIDMap.put(commandID, path);
			
			return commandID;
		}
	}
	
	public synchronized void getOUT(Path path, int commandID) {
		System.out.println(transferMap.toString());
		System.out.println(commandIDMap.toString());
		
		//remove locks
		transferMap.get(path).readLock().unlock();
		commandIDMap.remove(commandID);
		
		if (transferMap.get(path).getReadLockCount() == 0 && !transferMap.get(path).isWriteLocked())
			transferMap.remove(path);
		
		System.out.println(transferMap.toString());
		System.out.println(commandIDMap.toString());
	}
	
	public synchronized int putIN_ID(Path path) {
		int commandID = 0;
		
		while (commandIDMap.containsKey(commandID = generateID()));
		commandIDMap.put(commandID, path);
		
		writeQueue.add(commandID);
		
		return commandID;
	}
	
	public synchronized boolean putIN(Path path, int commandID) {
		if (writeQueue.peek() == commandID) {
			if (transferMap.containsKey(path)) {
				if (transferMap.get(path).writeLock().tryLock()) {
					
					writeQueue.poll();
					return true;
				} else
					return false;
			} else {
				transferMap.put(path, new ReentrantReadWriteLock());
				transferMap.get(path).writeLock().lock();
				
				writeQueue.poll();
				return true;
			}
		}
		return false;
	}
	
	public synchronized void putOUT(Path path, int commandID) {
		System.out.println(transferMap.toString());
		System.out.println(commandIDMap.toString());
		
		transferMap.get(path).writeLock().unlock();
		commandIDMap.remove(commandID);
		
		if (transferMap.get(path).getReadLockCount() == 0 && !transferMap.get(path).isWriteLocked())
			transferMap.remove(path);
		
		System.out.println(transferMap.toString());
		System.out.println(commandIDMap.toString());
	}
	
	public int generateID() {
		return new Random().nextInt(90000) + 10000;
	}
	
	public synchronized boolean delete(Path path) {
		return !transferMap.containsKey(path);
	}
}


//ReadWriteLock lock = new ReentrantReadWriteLock();
//lock.writeLock().lock();