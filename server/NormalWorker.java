package ftp.server;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.file.DirectoryNotEmptyException;
import java.nio.file.DirectoryStream;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class NormalWorker implements Runnable {
	private FTPServer ftpServer;
	private Socket nSocket;
	private Path path;
	private List<String> tokens;
	
	
	//Input
	InputStreamReader iStream;
	BufferedReader reader;
	//Data
	DataInputStream byteStream;
	//Output
	OutputStream oStream;
	DataOutputStream dStream;
	
	
	public NormalWorker(FTPServer ftpServer, Socket nSocket) throws Exception {
		this.ftpServer = ftpServer;
		this.nSocket = nSocket;
		path = Paths.get(System.getProperty("user.dir"));
		
		//streams
		iStream = new InputStreamReader(nSocket.getInputStream());
		reader = new BufferedReader(iStream);
		byteStream = new DataInputStream(nSocket.getInputStream());
		oStream = nSocket.getOutputStream();
		dStream = new DataOutputStream(oStream);
	}
	
	public void get() throws Exception {
		//not a directory or file
		if (Files.notExists(path.resolve(tokens.get(1)))) {
			dStream.writeBytes("get: " + tokens.get(1) + ": No such file or directory" + "\n");
		} 
		//is a directory
		else if (Files.isDirectory(path.resolve(tokens.get(1)))) {
			dStream.writeBytes("get: " + tokens.get(1) + ": Is a directory" + "\n");
		} 
		//transfer file
		else {
			//blank message
			dStream.writeBytes("\n");
			
			File file = new File(path.resolve(tokens.get(1)).toString());
			long fileSize = file.length();
			
			//send file size
			dStream.writeBytes(fileSize + "\n");
			
			//need to figure
			Thread.sleep(100);
			
			byte[] buffer = new byte[8192];
			try {
				BufferedInputStream in = new BufferedInputStream(new FileInputStream(file));
				int count = 0;
				while((count = in.read(buffer)) > 0)
					dStream.write(buffer, 0, count);
				
				in.close();
			} catch(Exception e) {
				System.out.println("transfer error: " + tokens.get(1));
			}
		}
	}
	
	public void put() throws Exception {
		//get file size
		long fileSize = Long.parseLong(reader.readLine());
		FileOutputStream f = new FileOutputStream(new File(path.resolve(tokens.get(1)).toString()));
		int count = 0;
		byte[] buffer = new byte[8192];
		long bytesReceived = 0;
		while(bytesReceived < fileSize) {
			count = byteStream.read(buffer);
			f.write(buffer, 0, count);
			bytesReceived += count;
		}
		f.close();
	}
	
	public void delete() throws Exception {
		try {
			boolean confirm = Files.deleteIfExists(path.resolve(tokens.get(1)));
			if (!confirm) {
				dStream.writeBytes("delete: cannot remove '" + tokens.get(1) + "': No such file" + "\n");
				dStream.writeBytes("\n");
			} else
				dStream.writeBytes("\n");
		} catch(DirectoryNotEmptyException enee) {
			dStream.writeBytes("delete: failed to remove `" + tokens.get(1) + "': Directory not empty" + "\n");
			dStream.writeBytes("\n");
		} catch(Exception e) {
			dStream.writeBytes("delete: failed to remove `" + tokens.get(1) + "'" + "\n");
			dStream.writeBytes("\n");
		}
	}
	
	public void ls() throws Exception {
		try {
			DirectoryStream<Path> dirStream = Files.newDirectoryStream(path);
			for (Path entry: dirStream)
				dStream.writeBytes(entry.getFileName() + "\n");
			dStream.writeBytes("\n");
		} catch(Exception e) {
			dStream.writeBytes("ls: failed to retrive contents" + "\n");
			dStream.writeBytes("\n");
		}
	}
	
	public void cd() throws Exception {
		try {
			//cd
			if (tokens.size() == 1) {
				path = Paths.get(System.getProperty("user.dir"));
				dStream.writeBytes("\n");
			}
			//cd ..
			else if (tokens.get(1).equals("..")) {
				if (path.getParent() != null)
					path = path.getParent();
				
				dStream.writeBytes("\n");
			}
			//cd somedirectory
			else {
				//not a directory or file
				if (Files.notExists(path.resolve(tokens.get(1)))) {
					dStream.writeBytes("cd: " + tokens.get(1) + ": No such file or directory" + "\n");
				} 
				//is a directory
				else if (Files.isDirectory(path.resolve(tokens.get(1)))) {
					path = path.resolve(tokens.get(1));
					dStream.writeBytes("\n");
				}
				//is a file
				else {
					dStream.writeBytes("cd: " + tokens.get(1) + ": Not a directory" + "\n");
				}
			}
		} catch (Exception e) {
			dStream.writeBytes("cd: " + tokens.get(1) + ": Error" + "\n");
		}
	}
	
	public void mkdir() throws Exception {
		try {
			Files.createDirectory(path.resolve(tokens.get(1)));
			dStream.writeBytes("\n");
		} catch(FileAlreadyExistsException falee) {
			dStream.writeBytes("mkdir: cannot create directory `" + tokens.get(1) + "': File or folder exists" + "\n");
		} catch(Exception e) {
			dStream.writeBytes("mkdir: cannot create directory `" + tokens.get(1) + "': Permission denied" + "\n");
		}
	}
	
	public void pwd() throws Exception {
		//send path
		dStream.writeBytes(path + "\n");
	}
	
	public void quit() throws Exception {
		//close socket
		nSocket.close();
	}
	
	public void run() {
		while (true) {
			try {
				//check every 10 ms for input
				while (!reader.ready())
					Thread.sleep(10);
				
				//capture and parse input
				tokens = new ArrayList<String>();
				String command = reader.readLine();
				Scanner tokenize = new Scanner(command);
				//gets command
				if (tokenize.hasNext())
				    tokens.add(tokenize.next());
				//gets rest of string after the command; this allows filenames with spaces: 'file1 test.txt'
				if (tokenize.hasNext())
					tokens.add(command.substring(tokens.get(0).length()).trim());
				tokenize.close();
				if (Main.DEBUG) System.out.println(tokens.toString());
				
				//command selector
				switch(tokens.get(0)) {
					case "get": 	get();		break;
					case "put": 	put();		break;
					case "delete": 	delete();	break;
					case "ls": 		ls();		break;
					case "cd": 		cd();		break;
					case "mkdir": 	mkdir();	break;
					case "pwd": 	pwd();		break;
					case "quit": 	quit();		break;
					default:
						System.out.println("invalid command");
				}
			} catch (Exception e) {
				e.printStackTrace(); //TODO
			}
		}
	}
}