package client;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;


public class MiniServer {

	private ServerSocket serverSocket;
	private boolean toClose = false;

	public MiniServer(int port) throws IOException {
		serverSocket = new ServerSocket(port);
	}

	public void start() {
		new Thread() {
			public void run() {
				while (!toClose) {
					try (Socket socket = serverSocket.accept()) {

						try (DataOutputStream out = new DataOutputStream(socket.getOutputStream());
								DataInputStream in = new DataInputStream(socket.getInputStream())) {

							String fromDir = in.readUTF();
							
							if (!new File(fromDir).exists()) {
								out.writeInt(1);
								out.writeUTF(fromDir + " doesn't exist or cannot open.");
								out.flush();
								return;
							}
							try (InputStream fileIn = new BufferedInputStream(new FileInputStream(fromDir))) {
								out.writeInt(0);
								int count;
								byte[] buffer = new byte[8192];
								while ((count = fileIn.read(buffer)) > 0) {
									out.write(buffer, 0, count);
									out.flush();
								}
								out.flush();
							} catch (Exception e) {
								out.writeInt(1);
								out.writeUTF("File doesn't exist or cannot open.");
								out.flush();
							}

						} catch (IOException ioe) {
							ioe.printStackTrace();
							System.out.println("A problem with mini server socket occured.");
						}
					} catch (IOException e1) {
						e1.printStackTrace();
						System.out.println("A problem with mini server socket occured.");
					}
				}
			}
		}.start();
	}

	public void close() {
		toClose = true;
	}
}
