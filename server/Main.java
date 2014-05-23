/*
 * Copyright (c) 2014, vincentclee <ssltunnelnet@gmail.com>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *   - Redistributions of source code must retain the above copyright
 *     notice, this list of conditions and the following disclaimer.
 *
 *   - Redistributions in binary form must reproduce the above copyright
 *     notice, this list of conditions and the following disclaimer in the
 *     documentation and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
 * IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO,
 * THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED.  IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package ftp.server;

/**
 * FTP Server Launcher
 * @author Will Henry
 * @author Vincent Lee
 * @version 1.0
 * @since March 26, 2014
 */

import java.net.BindException;
import java.net.ServerSocket;

public class Main {
	public static final boolean DEBUG = false;
	private static ServerSocket nSocket, tSocket;
	
	/**
	 * Repeatedly listens for incoming messages on TCP port normal commands (nport) and terminate port (tport)
	 * @param args nport, tport
	 */
	public static void main(String[] args) {
		//number of arguments
		if (args.length != 2) {
			System.out.println("error: Invalid number of arguments");
			return;
		}
		
		////////////////////////////
		// port range: 1 to 65535 //
		////////////////////////////
		
		//nPort port number
		int nPort = 0;
		try {
			nPort = Integer.parseInt(args[0]);
			if (nPort < 1 || nPort > 65535) throw new Exception();
		} catch (NumberFormatException nfe) {
			System.out.println("error: Invalid nport number");
			return;
		} catch (Exception e) {
			System.out.println("error: Invalid nport range, valid ranges: 1-65535");
			return;
		}
		
		//tPort port number
		int tPort = 0;
		try {
			tPort = Integer.parseInt(args[1]);
			if (tPort < 1 || tPort > 65535) throw new Exception();
		} catch (NumberFormatException nfe) {
			System.out.println("error: Invalid tport number");
			return;
		} catch(Exception e) {
			System.out.println("error: Invalid nport range, valid ranges: 1-65535");
			return;
		}
		
		//port numbers must be different
		if (nPort == tPort) {
			System.out.println("error: nPort and tPort must be port numbers");
			return;
		}
		
		//listening sockets
		try {
			nSocket = new ServerSocket(nPort);
			tSocket = new ServerSocket(tPort);
		} catch(BindException be) {
			System.out.println("error: one or more ports are already in use");
			return;
		} catch(Exception e) {
			System.out.println("error: server could not be started");
			return;
		}
		
		/////////////////////////
		// FTP Server Launcher //
		/////////////////////////
		
		try {
			//shared memory object
			FTPServer ftpServer = new FTPServer();
			
			//two threads, one for each socket
			(new Thread(new NormalDaemon(ftpServer, nSocket))).start();
			(new Thread(new TerminateDaemon(ftpServer, tSocket))).start();
		} catch (Exception e) {
			System.out.println("ftp.server.Main");
			e.printStackTrace(); //TODO
		}
	}
}