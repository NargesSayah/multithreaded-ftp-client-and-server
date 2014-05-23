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

package ftp.client;

/**
 * FTP Client Launcher
 * @author Will Henry
 * @author Vincent Lee
 * @version 1.0
 * @since March 26, 2014
 */

import java.net.ConnectException;
import java.net.InetAddress;
import java.net.SocketTimeoutException;

public class Main {
	public static final boolean DEBUG = false;
	public static final String PROMPT = "mytftp>";
	public static final String EXIT_MESSAGE = "FTP session ended. Bye!";
	public static int nPort, tPort;
	public static String hostname;
	
	/**
	 * FTP client launcher which connects to remote host
	 * @param args machineName, nPort, tPort
	 */
	public static void main(String[] args) {
		//number of arguments
		if (args.length != 3) {
			System.out.println("error: Invalid number of arguments");
			return;
		}
		
		//hostname
		try {
			InetAddress.getByName(args[0]);
			hostname = args[0];
		} catch (Exception e) {
			System.out.println("error: hostname does not resolve to an IP address");
			return;
		}
		
		////////////////////////////
		// port range: 1 to 65535 //
		////////////////////////////
		
		//nPort port number
		try {
			nPort = Integer.parseInt(args[1]);
			if (nPort < 1 || nPort > 65535) throw new Exception();
		} catch (NumberFormatException nfe) {
			System.out.println("error: Invalid nport number");
			return;
		} catch (Exception e) {
			System.out.println("error: Invalid nport range, valid ranges: 1-65535");
			return;
		}
		
		//tPort port number
		try {
			tPort = Integer.parseInt(args[2]);
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
		
		/////////////////////////
		// FTP Client Launcher //
		/////////////////////////
		
		try {
			//shared memory object
			FTPClient ftpClient = new FTPClient();
			
			//initial starting thread
			(new Thread(new Worker(ftpClient, hostname, nPort))).start();
		} catch (SocketTimeoutException ste) {
			System.out.println("error: host could not be reached");
		} catch (ConnectException ce) {
			System.out.println("error: no running FTP at remote host");
		} catch (Exception e) {
			System.out.println("error: program quit unexpectedly");
		}
	}
}