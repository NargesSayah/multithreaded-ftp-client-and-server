package ftp.client;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class Worker implements Runnable {
	private FTPClient ftpClient;
	private Socket socket;
	private Path path, serverPath;
	private List<String> tokens;
	private boolean input;
	
	//Stream
	private InputStreamReader iStream;
	private BufferedReader reader;
	private DataInputStream byteStream; 
	private OutputStream oStream;
	private DataOutputStream dStream;
	
	
	public Worker(FTPClient ftpClient, String hostname, int nPort) throws Exception {
		this.ftpClient = ftpClient;
		
		//Connect to server
		InetAddress ip = InetAddress.getByName(hostname);
		socket = new Socket();
		socket.connect(new InetSocketAddress(ip.getHostAddress(), nPort), 1000);
		
		//Streams
		initiateStream();
		
		//thread with keyboard input
		input = true;
		
		//Set current working directory
		path = Paths.get(System.getProperty("user.dir"));
		System.out.println("Connected to: " + ip);
	}
	
	public Worker(FTPClient ftpClient) {
		this.ftpClient = ftpClient;
	}
	
	public void initiateStream() {
		try {
			//Input
			iStream = new InputStreamReader(socket.getInputStream());
			reader = new BufferedReader(iStream);
			
			//Data
			byteStream = new DataInputStream(socket.getInputStream());
			
			//Output
			oStream = socket.getOutputStream();
			dStream = new DataOutputStream(oStream);
			
			//get server directory
			dStream.writeBytes("pwd" + "\n");
			
			//set server directory
			String get_line;
			if (!(get_line = reader.readLine()).equals("")) {
				System.out.println(get_line);
				serverPath = Paths.get(get_line);
			}
		} catch (Exception e) {
			System.out.println("stream initiation error"); //TODO
		}
	}
	
	public void get() throws Exception {
		if (tokens.size() != 2) {
			invalid();
			return;
		}
		
		//send command
		dStream.writeBytes("get " + tokens.get(1) + "\n");
		
		//error messages
		String get_line;
		if (!(get_line = reader.readLine()).equals("")) {
			System.out.println(get_line);
			return;
		}
		
		//get file size
		long fileSize = Long.parseLong(reader.readLine());
		FileOutputStream f = new FileOutputStream(new File(tokens.get(1)));
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
	
	public void put() throws Exception {
		if (tokens.size() != 2) {
			invalid();
			return;
		}
		
		//not a directory or file
		if (Files.notExists(path.resolve(tokens.get(1)))) {
			System.out.println("put: " + tokens.get(1) + ": No such file or directory");
		} 
		//is a directory
		else if (Files.isDirectory(path.resolve(tokens.get(1)))) {
			System.out.println("put: " + tokens.get(1) + ": Is a directory");
		}
		//transfer file
		else {
			//send command
			dStream.writeBytes("put " + tokens.get(1) + "\n");
			
			File file = new File(path.resolve(tokens.get(1)).toString());
			long fileSize1 = file.length();
			
			//send file size
			dStream.writeBytes(fileSize1 + "\n");
			
			//need to figure
			Thread.sleep(100);
			
			byte[] buffer1 = new byte[8192];
			try {
				BufferedInputStream in = new BufferedInputStream(new FileInputStream(file));
				int count2 = 0;
				while((count2 = in.read(buffer1)) > 0)
					dStream.write(buffer1, 0, count2);
				
				in.close();
			} catch(Exception e){
				System.out.println("transfer error: " + tokens.get(1));
			}
		}
	}
	
	public void delete() throws Exception {
		//only two arguments
		if (tokens.size() != 2) {
			invalid();
			return;
		}
		
		//not backgroundable
		if (tokens.get(1).endsWith(" &")) {
			notSupported();
			return;
		}
		
		//send command
		dStream.writeBytes("delete " + tokens.get(1) + "\n");
		
		//messages
		String delete_line;
		while (!(delete_line = reader.readLine()).equals(""))
		    System.out.println(delete_line);
	}
	
	public void ls() throws Exception {
		//only one argument
		if (tokens.size() != 1) {
			invalid();
			return;
		}
		
		//send command
		dStream.writeBytes("ls" + "\n");
		
		//messages
		String ls_line;
		while (!(ls_line = reader.readLine()).equals(""))
		    System.out.println(ls_line);
	}
	
	public void cd() throws Exception {
		//up to two arguments
		if (tokens.size() > 2) {
			invalid();
			return;
		}
		
		//not backgroundable
		if (tokens.get(tokens.size()-1).endsWith(" &")) {
			notSupported();
			return;
		}
		
		//send command
		if (tokens.size() == 1) //allow "cd" goes back to home directory
			dStream.writeBytes("cd" + "\n");
		else
			dStream.writeBytes("cd " + tokens.get(1) + "\n");
		
		//messages
		String cd_line;
		if (!(cd_line = reader.readLine()).equals(""))
			System.out.println(cd_line);
		
		//get server directory
		dStream.writeBytes("pwd" + "\n");
		
		//set server directory
		String get_line;
		if (!(get_line = reader.readLine()).equals("")) {
			System.out.println(get_line);
			serverPath = Paths.get(get_line);
		}
	}
	
	public void mkdir() throws Exception {
		//only two arguments
		if (tokens.size() != 2) {
			invalid();
			return;
		}
		
		//not backgroundable
		if (tokens.get(1).endsWith(" &")) {
			notSupported();
			return;
		}
		
		//send command
		dStream.writeBytes("mkdir " + tokens.get(1) + "\n");
		
		//messages
		String mkdir_line;
		if (!(mkdir_line = reader.readLine()).equals(""))
			System.out.println(mkdir_line);
	}
	
	public void pwd() throws Exception {
		//only one argument
		if (tokens.size() != 1) {
			invalid();
			return;
		}
		
		//send command
		dStream.writeBytes("pwd" + "\n");
		
		//message
		System.out.println(reader.readLine());
	}
	
	public void quit() throws Exception {
		//only one argument
		if (tokens.size() != 1) {
			invalid();
			return;
		}
		
		//send command
		dStream.writeBytes("quit" + "\n");
	}
	
	public void terminate() throws Exception {
		//only two arguments
		if (tokens.size() != 2) {
			invalid();
			return;
		}
		
		//not backgroundable
		if (tokens.get(1).endsWith(" &")) {
			notSupported();
			return;
		}
	}
	
	public void notSupported() {
		System.out.println("This command is not backgroundable.");
	}
	
	public void invalid() {
		System.out.println("Invalid Arguments");
		System.out.println("Try `help' for more information.");
	}
	
	public void help() {
		System.out.println("Available commands:");
		System.out.println(" get (get <remote_filename>) \t\t  – Copy file with the name <remote_filename> from remote directory to local directory.");
		System.out.println(" put (put <local_filename>) \t\t  – Copy file with the name <local_filename> from local directory to remote directory.");
		System.out.println(" delete (delete <remote_filename>) \t  – Delete the file with the name <remote_filename> from the remote directory.");
		System.out.println(" ls (ls) \t\t\t\t  – List the files and subdirectories in the remote directory.");
		System.out.println(" cd (cd <remote_direcotry_name> or cd ..) – Change to the <remote_direcotry_name> on the remote machine or change to the parent directory of the current directory.");
		System.out.println(" mkdir (mkdir <remote_directory_name>) \t  – Create directory named <remote_direcotry_name> as the sub-directory of the current working directory on the remote machine.");
		System.out.println(" pwd (pwd) \t\t\t\t  – Print the current working directory on the remote machine.");
		System.out.println(" terminate (terminate <command-ID> \t  – terminate the command identiied by <command-ID>.");
		System.out.println(" quit (quit) \t\t\t\t  – End the FTP session.");
	}
	
	public void input() {
		try {
			//keyboard input
			Scanner input = new Scanner(System.in);
			String command;
			
			do {
				//get input
				System.out.print(Main.PROMPT);
				command = input.nextLine();
				command = command.trim();
				
				//parse input into tokens
				tokens = new ArrayList<String>();
				Scanner tokenize = new Scanner(command);
				//gets command
				if (tokenize.hasNext())
				    tokens.add(tokenize.next());
				//gets rest of string after the command; this allows filenames with spaces: 'file1 test.txt'
				if (tokenize.hasNext())
					tokens.add(command.substring(tokens.get(0).length()).trim());
				tokenize.close();
				if (Main.DEBUG) System.out.println(tokens);
				
				//allows for blank enter
				if (tokens.isEmpty())
					continue;
				
				//command selector
				switch(tokens.get(0)) {
					case "get": 		get(); 			break;
					case "put": 		put(); 			break;
					case "delete": 		delete(); 		break;
					case "ls": 			ls(); 			break;
					case "cd": 			cd(); 			break;
					case "mkdir": 		mkdir(); 		break;
					case "pwd": 		pwd(); 			break;
					case "quit": 		quit(); 		break;
					case "help": 		help(); 		break;
					case "terminate":	terminate();	break;
					default:
						System.out.println("unrecognized command '" + tokens.get(0) + "'");
						System.out.println("Try `help' for more information.");
						
				}
			} while (!command.equalsIgnoreCase("quit"));
			input.close();
			
			
			System.out.println(Main.EXIT_MESSAGE);
		} catch (Exception e) {
			System.out.println("error: disconnected from host");
			e.printStackTrace(); //TODO
		}
	}
	
	public void run() {
		if (input) {
			input();
		}
	}
}