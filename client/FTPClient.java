package ftp.client;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class FTPClient {
	private Set<Path> transferSet;
	private Map<Integer, Path> commandIDMap;
	
	public FTPClient() {
		transferSet = new HashSet<Path>();
		commandIDMap = new HashMap<Integer, Path>();
	}
	
	public synchronized boolean transfer(Path path) {
		return !transferSet.contains(path);
	}
	
	public synchronized void transferIN(Path path, int commandID) {
		transferSet.add(path);
		commandIDMap.put(commandID, path);
	}
	
	public synchronized void transferOUT(Path path, int commandID) {
		try {
			transferSet.remove(path);
			commandIDMap.remove(commandID);
		} catch(Exception e) {}
	}
	
	public synchronized boolean quit() {
		return transferSet.isEmpty();
	}
}
