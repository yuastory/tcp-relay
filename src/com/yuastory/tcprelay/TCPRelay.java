package com.yuastory.tcprelay;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * 
 * @author yuastory
 * @see https://github.com/yuastory/tcprelay
 */
public class TCPRelay {
	private final String internalHost;
	private final int internalPort, externalPort;
	private ServerSocket relaySocket;

	public TCPRelay(String internalHost, int internalPort, int externalPort) {
		this.internalHost = internalHost;
		this.internalPort = internalPort;
		this.externalPort = externalPort;
	}

	private void closeSocket(Socket socket) {
		if (socket != null && !socket.isClosed()) {
			try {
				socket.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	private class RelayRunnable implements Runnable {
		InputStream in;
		OutputStream out;

		public RelayRunnable(InputStream in, OutputStream out) {
			this.in = in;
			this.out = out;
		}

		public void run() {
			int n;
			byte[] buffer = new byte[1024];
			try {
				while ((n = in.read(buffer)) != -1) {
					out.write(buffer, 0, n);
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	public void startRelay() {
		try {
			relaySocket = new ServerSocket(externalPort);
		} catch (IOException e) {
			e.printStackTrace();
			return;
		}

		Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
			public void run() {
				try {
					relaySocket.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}));

		while (true) {
			Socket client = null;
			Socket service = null;
			try {
				client = relaySocket.accept();
				service = new Socket(internalHost, internalPort);

				Thread threadRecvFromClient = new Thread(new RelayRunnable(client.getInputStream(), service.getOutputStream()));
				Thread threadSendToClient = new Thread(new RelayRunnable(service.getInputStream(), client.getOutputStream()));
				threadRecvFromClient.start();
				threadSendToClient.start();
			} catch (IOException e) {
				e.printStackTrace();
				closeSocket(client);
				closeSocket(service);
			}
		}
	}
}
