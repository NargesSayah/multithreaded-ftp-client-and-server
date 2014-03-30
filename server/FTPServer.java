package ftp.server;

/**
 * 
 * @author Will Henry
 * @author Vincent Lee
 * @version 1.0
 * @since March 26, 2014
 */

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class FTPServer {
	private Map<Path, ReentrantReadWriteLock> transferMap;
	private Map<Integer, Path> commandIDMap;
	private Queue<Integer> writeQueue;
	private Set<Integer> terminateSet;
	
	public FTPServer() {
		transferMap = new HashMap<Path, ReentrantReadWriteLock>();
		commandIDMap = new HashMap<Integer, Path>();
		writeQueue = new LinkedList<Integer>();
		terminateSet = new HashSet<Integer>();
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
//		System.out.println(transferMap.toString());
//		System.out.println(commandIDMap.toString());
		
		try {
			//remove locks
			transferMap.get(path).readLock().unlock();
			commandIDMap.remove(commandID);
			
			if (transferMap.get(path).getReadLockCount() == 0 && !transferMap.get(path).isWriteLocked())
				transferMap.remove(path);
		} catch (Exception e) {}
		
//		System.out.println(transferMap.toString());
//		System.out.println(commandIDMap.toString());
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
//		System.out.println(transferMap.toString());
//		System.out.println(commandIDMap.toString());
		
		try {
			transferMap.get(path).writeLock().unlock();
			commandIDMap.remove(commandID);
			
			if (transferMap.get(path).getReadLockCount() == 0 && !transferMap.get(path).isWriteLocked())
				transferMap.remove(path);
		} catch (Exception e) {}
		
//		System.out.println(transferMap.toString());
//		System.out.println(commandIDMap.toString());
	}
	
	public int generateID() {
		return new Random().nextInt(90000) + 10000;
	}
	
	public synchronized boolean delete(Path path) {
		return !transferMap.containsKey(path);
	}
	
	public synchronized void terminate(int commandID) {
		terminateSet.add(commandID);
	}

	public boolean terminateGET(Path path, int commandID) {
		try {
			if (terminateSet.contains(commandID)) {
				terminateSet.remove(commandID);
				commandIDMap.remove(commandID);
				transferMap.get(path).readLock().unlock();
				
				if (transferMap.get(path).getReadLockCount() == 0 && !transferMap.get(path).isWriteLocked())
					transferMap.remove(path);
				return true;
			}
		} catch (Exception e) {}
		
		return false;
	}
	
	public boolean terminatePUT(Path path, int commandID) {
		try {
			if (terminateSet.contains(commandID)) {
				terminateSet.remove(commandID);
				commandIDMap.remove(commandID);
				transferMap.get(path).writeLock().unlock();
				Files.deleteIfExists(path);
				
				if (transferMap.get(path).getReadLockCount() == 0 && !transferMap.get(path).isWriteLocked())
					transferMap.remove(path);
				return true;
			}
		} catch (Exception e) {}
		
		return false;
	}
}