package ftp.server;

import java.net.ServerSocket;

public class NormalDaemon implements Runnable {
	private FTPServer ftpServer;
	private ServerSocket nSocket;
	
	public NormalDaemon(FTPServer ftpServer, ServerSocket nSocket) {
		this.ftpServer = ftpServer;
		this.nSocket = nSocket;
	}
	
	public void run() {
		while (true) {
			try {
				System.out.println("NormalDaemon");
				(new Thread(new NormalWorker(ftpServer, nSocket.accept()))).start();
			} catch (Exception e) {
				System.out.println("Normal threadID: " + Thread.currentThread().getId() + " could not start worker");
				e.printStackTrace();
			}
		}
	}
}