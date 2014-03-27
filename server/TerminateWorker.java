package ftp.server;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class TerminateWorker implements Runnable {
	private FTPServer ftpServer;
	private Socket tSocket;
	
	public TerminateWorker(FTPServer ftpServer, Socket tSocket) {
		this.ftpServer = ftpServer;
		this.tSocket = tSocket;
	}
	
	public void run() {
		try {
			//Input
			InputStreamReader iStream = new InputStreamReader(tSocket.getInputStream());
			BufferedReader reader = new BufferedReader(iStream);
			
			//check every 10 ms for input
			while (!reader.ready())
				Thread.sleep(10);
			
			//capture and parse input
			List<String> tokens = new ArrayList<String>();
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
				case "terminate":
					ftpServer.terminate(tokens.get(1));
				default:
					System.out.println("TerminateWorker invalid command");
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		
	}
}