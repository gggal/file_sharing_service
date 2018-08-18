package server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Tracker {
	private Map<Socket, User> users = new HashMap<Socket, User>();
	private Map<String, Set<String>> uploads = new HashMap<String, Set<String>>();
	private ExecutorService executor = Executors.newCachedThreadPool();
	private ServerSocket serverSocket;

	public Tracker(int port) throws IOException {
		serverSocket = new ServerSocket(port);

		new Thread() {
			public void run() {
				Socket socket;
				while (true) {
					try {
						socket = serverSocket.accept();
						executor.execute(new TrackerWorker(socket, users, uploads));
						Thread.sleep(10);
					} catch (Exception e) {
						System.out.println("Server is down.");
						if(!serverSocket.isClosed()) {
							try {
								serverSocket.close();
							} catch (IOException e1) {
								e1.printStackTrace();
							}
						}
						e.printStackTrace();
						break;
					}
				}
			}
		}.start();
	}

	public Map<Socket, User> getUsers() {
		return users;
	}

	public Map<String, Set<String>> getUploads() {
		return uploads;
	}

	public void close() throws IOException {
		serverSocket.close();
	}

	public static void main(String[] args) {
		@SuppressWarnings("resource")
		Scanner sc = new Scanner(System.in);
		String s;
		try {
			Tracker tracker = new Tracker(Integer.parseInt(args[0])); //Tracker(1111);

			s = sc.next();
			while (true) {
				if ("close".equals(s)) {
					tracker.close();
					break;
				}
				s = sc.next();
			}
		} catch (IOException e) {
			System.out.println("Problem with server on port " + args[0]);
		}

		return;
	}
}
