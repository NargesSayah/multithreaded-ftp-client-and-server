package ftp.server;

import java.net.ServerSocket;

public class TerminateDaemon implements Runnable {
	private FTPServer ftpServer;
	private ServerSocket tSocket;
	
	public TerminateDaemon(FTPServer ftpServer, ServerSocket tSocket) {
		this.ftpServer = ftpServer;
		this.tSocket = tSocket;
	}
	
	public void run() {
		while (true) {
			try {
				System.out.println("TerminateDaemon");
				(new Thread(new TerminateWorker(ftpServer, tSocket.accept()))).start();
			} catch (Exception e) {
				System.out.println("Terminate threadID: " + Thread.currentThread().getId() + " could not start worker");
				e.printStackTrace();
			}
		}
	}
}